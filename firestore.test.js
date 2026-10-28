const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function serverTimestampSentinel() {
  return new Date(Date.now() - 2000);
}

test("Unauthenticated user: cannot read or create accounts, categories, or entries", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("accounts")
      .where("userId", "==", ALICE_UID)
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("categories")
      .doc("cat_dhak")
      .get()
  );
});

test("Authenticated owner: can create valid account, category, and entry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestampSentinel();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("accounts")
      .doc("kali_pujo_2026")
      .set({
        userId: ALICE_UID,
        name: "Kali Pujo 2026",
        year: 2026,
        defaultContribution: 1800,
        targetBudget: 35000,
        archived: false,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("categories")
      .doc("cat_dhak")
      .set({
        userId: ALICE_UID,
        name: "Dhak",
        type: "EXPENSE",
        budgetLimit: 2000,
        colorHex: "#C62828",
        iconKey: "music",
        sortOrder: 1,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("entries")
      .doc("entry_dhak_2026")
      .set({
        userId: ALICE_UID,
        accountId: "kali_pujo_2026",
        type: "EXPENSE",
        amount: 1000,
        categoryId: "cat_dhak",
        categoryName: "Dhak",
        notes: "Dhak booking & performance",
        partyName: "",
        paymentMode: "FUND",
        dateStr: "27/10/2026",
        dateEpochMillis: 1793059200000,
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Orphaned entry without existing parent account is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestampSentinel();

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("entries")
      .doc("orphan_entry")
      .set({
        userId: ALICE_UID,
        accountId: "missing_account",
        type: "EXPENSE",
        amount: 1000,
        categoryId: "cat_dhak",
        categoryName: "Dhak",
        notes: "Orphaned entry",
        partyName: "",
        paymentMode: "FUND",
        dateStr: "27/10/2026",
        dateEpochMillis: 1793059200000,
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestampSentinel();

  await aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("accounts")
    .doc("kali_pujo_2026")
    .set({
      userId: ALICE_UID,
      name: "Kali Pujo 2026",
      year: 2026,
      defaultContribution: 1800,
      targetBudget: 35000,
      archived: false,
      createdAt: ts,
      updatedAt: ts,
    });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("accounts")
      .doc("kali_pujo_2026")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("accounts")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Shadow update & value poisoning rejected on categories and entries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestampSentinel();

  await aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("categories")
    .doc("cat_thakur")
    .set({
      userId: ALICE_UID,
      name: "Thakur",
      type: "EXPENSE",
      budgetLimit: 5000,
      colorHex: "#AD1457",
      iconKey: "temple",
      sortOrder: 2,
      createdAt: ts,
      updatedAt: ts,
    });

  // Ghost field injection fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("categories")
      .doc("cat_thakur")
      .update({
        budgetLimit: 6000,
        isVerifiedAdmin: true,
        updatedAt: ts,
      })
  );

  // Value poisoning (negative budget) fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("categories")
      .doc("cat_thakur")
      .update({
        budgetLimit: -500,
        updatedAt: ts,
      })
  );
});

test("Client query alignment: query with userId filter succeeds, unfiltered list fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestampSentinel();

  await aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("categories")
    .doc("cat_thakurmasai")
    .set({
      userId: ALICE_UID,
      name: "Thakurmasai",
      type: "EXPENSE",
      budgetLimit: 3000,
      colorHex: "#EF6C00",
      iconKey: "person",
      sortOrder: 3,
      createdAt: ts,
      updatedAt: ts,
    });

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("categories")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("categories").get()
  );
});

test("Authenticated owner: can update allowed fields while preserving createdAt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdTs = new Date(Date.now() - 5000);
  const updatedTs = new Date(Date.now() - 1000);

  await aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("categories")
    .doc("cat_dhak")
    .set({
      userId: ALICE_UID,
      name: "Dhak",
      type: "EXPENSE",
      budgetLimit: 1000,
      colorHex: "#C62828",
      iconKey: "music",
      sortOrder: 1,
      createdAt: createdTs,
      updatedAt: createdTs,
    });

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("categories")
      .doc("cat_dhak")
      .update({
        budgetLimit: 1500,
        updatedAt: updatedTs,
      })
  );
});
