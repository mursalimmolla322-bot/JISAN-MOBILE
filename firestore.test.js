const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_owner_1";
const ALICE_UID = "alice_customer_1";
const BOB_UID = "bob_customer_2";
const OWNER_EMAIL = "mursalimmolla322@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProductPayload(overrides = {}) {
  const now = new Date();
  return {
    name: "Samsung Galaxy M35 5G",
    description: "6000mAh Battery, 120Hz sAMOLED Display",
    categoryId: "cat_phones",
    categoryName: "Smartphones",
    brand: "Samsung",
    sku: "SAM-M35-128",
    mrp: 19999,
    sellingPrice: 12999,
    discountPercent: 35,
    stockQuantity: 50,
    isPublished: true,
    isFeatured: true,
    salesCount: 0,
    ratingSum: 0,
    ratingCount: 0,
    imageUrls: ["data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/"],
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validUserPayload(uid, overrides = {}) {
  const now = new Date();
  return {
    userId: uid,
    name: "Customer User",
    email: `${uid}@example.com`,
    phone: "9876543210",
    photoUrl: "",
    referralCode: `JISAN${uid.slice(0, 5).toUpperCase()}`,
    referredByCode: "",
    referralRewardEarned: 0,
    isBlocked: false,
    preferredLanguage: "en",
    darkMode: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validOrderPayload(uid, overrides = {}) {
  const now = new Date();
  return {
    userId: uid,
    customerName: "Alice",
    customerEmail: "alice@example.com",
    customerPhone: "9876543210",
    shippingAddressLine: "12 MG Road",
    shippingCity: "Kolkata",
    shippingState: "West Bengal",
    shippingPincode: "700001",
    items: [
      {
        productId: "prod_1",
        productName: "Samsung Galaxy M35 5G",
        imageUrl: "",
        unitPrice: 12999,
        quantity: 1,
        lineTotal: 12999,
      },
    ],
    subtotal: 12999,
    discountAmount: 0,
    couponCode: "",
    deliveryCharge: 0,
    totalAmount: 12999,
    paymentMethod: "COD",
    paymentStatus: "PENDING",
    transactionId: "",
    orderStatus: "PENDING",
    trackingCarrier: "",
    trackingNumber: "",
    trackingNote: "",
    estimatedDelivery: "",
    idempotencyKey: `idem_${uid}_001`,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot list products or orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("products").where("isPublished", "==", true).get()
  );
  await assertFails(unauthDb.collection("orders").get());
});

test("Email spoofing attack without email_verified=true is rejected for admin actions", async () => {
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: OWNER_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertFails(
    spoofDb.collection("products").doc("prod_spoof").set(validProductPayload())
  );
});

test("Admin can create/update product and multiple customers see exact same published product", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: OWNER_EMAIL,
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(
    adminDb.collection("products").doc("prod_1").set(validProductPayload())
  );

  // Update price as admin
  await assertSucceeds(
    adminDb.collection("products").doc("prod_1").update({
      sellingPrice: 11999,
      updatedAt: new Date(),
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const aliceSnap = await assertSucceeds(
    aliceDb.collection("products").where("isPublished", "==", true).get()
  );
  const bobSnap = await assertSucceeds(
    bobDb.collection("products").where("isPublished", "==", true).get()
  );

  if (aliceSnap.size !== 1 || bobSnap.size !== 1) {
    throw new Error("Expected both customers to receive the same 1 product");
  }
  if (aliceSnap.docs[0].data().sellingPrice !== 11999) {
    throw new Error("Expected updated sellingPrice 11999");
  }
});

test("Customer cannot alter product price, publish state, or arbitrarily increase stock", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("products")
      .doc("prod_1")
      .set(validProductPayload());
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("products").doc("prod_1").update({
      sellingPrice: 1,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    aliceDb.collection("products").doc("prod_1").update({
      stockQuantity: 999,
      salesCount: 0,
      updatedAt: new Date(),
    })
  );
});

test("Customer atomic stock decrement succeeds only with valid decrement and matching salesCount", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("products")
      .doc("prod_1")
      .set(validProductPayload({ stockQuantity: 5, salesCount: 0 }));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("products").doc("prod_1").update({
      stockQuantity: 3,
      salesCount: 2,
      updatedAt: new Date(),
    })
  );

  // Cannot decrement below 0
  await assertFails(
    aliceDb.collection("products").doc("prod_1").update({
      stockQuantity: -1,
      salesCount: 4,
      updatedAt: new Date(),
    })
  );
});

test("PII Isolation: Customer cannot read another customer profile or orders", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .set(validUserPayload(BOB_UID));
    await ctx
      .firestore()
      .collection("orders")
      .doc("order_bob_1")
      .set(validOrderPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("orders").doc("order_bob_1").get());
  await assertFails(aliceDb.collection("orders").get()); // missing userId filter
  await assertSucceeds(
    aliceDb.collection("orders").where("userId", "==", ALICE_UID).get()
  );
});

test("Customer cannot escalate own role or unblock self", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isBlocked: true,
      updatedAt: new Date(),
    })
  );

  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      phone: "9876543210",
      displayName: "Alice",
      pinHash: "a".repeat(64),
      pinSalt: "salt1234",
      isPrimaryOwner: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Anti-self-referral enforced at rule level", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("referrals").doc(ALICE_UID).set({
      referrerUid: ALICE_UID,
      referrerCode: "JISANALICE",
      referredUid: ALICE_UID,
      referredEmail: "alice@example.com",
      status: "REGISTERED",
      rewardAmount: 50,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("referrals").doc(ALICE_UID).set({
      referrerUid: BOB_UID,
      referrerCode: "JISANBOB",
      referredUid: ALICE_UID,
      referredEmail: "alice@example.com",
      status: "REGISTERED",
      rewardAmount: 50,
      createdAt: now,
      updatedAt: now,
    })
  );
});
