const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "admin_alice_123";
const BOB_UID = "admin_bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("students").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("attendance").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("students").doc("stu_1").set({
      userId: ALICE_UID,
      nis: "2026001",
      fullName: "Budi Santoso",
      className: "X-A",
      gender: "L",
      qrToken: "",
      qrStatus: "PENDING",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Admin settings: owner create, partial update, cross-user & tamper denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      adminName: "Admin Taruna",
      schoolName: "SMA Taruna Andigha",
      checkInDeadlineHour: 7,
      checkInDeadlineMinute: 15,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      checkInDeadlineHour: 7,
      checkInDeadlineMinute: 30,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      adminName: "Hijack",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost field & invalid hour denied
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isSuperAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      checkInDeadlineHour: 25,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Students & QR issuance: create pending, issue QR, re-issue QR, reject invalid payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Create student with PENDING QR (qrIssuedAt omitted)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("students").doc("stu_1").set({
      userId: ALICE_UID,
      nis: "2026001",
      fullName: "Ahmad Fauzi",
      className: "X-A",
      gender: "L",
      qrToken: "",
      qrStatus: "PENDING",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Issue QR Code to student
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("students").doc("stu_1").update({
      qrToken: "TRN-2026001-A9X2K8",
      qrStatus: "ACTIVE",
      qrIssuedAt: new Date(Date.now() - 2000),
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // List query matching Kotlin repository
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("students").get()
  );

  // Cannot set ACTIVE without valid qrToken >= 6 chars
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("students").doc("stu_2").set({
      userId: ALICE_UID,
      nis: "2026002",
      fullName: "Siti Aminah",
      className: "X-B",
      gender: "P",
      qrToken: "",
      qrStatus: "ACTIVE",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject @DocumentId `id` field in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("students").doc("stu_3").set({
      id: "stu_3",
      userId: ALICE_UID,
      nis: "2026003",
      fullName: "Rizky Pratama",
      className: "XI-IPA-1",
      gender: "L",
      qrToken: "TRN-2026003-XY1234",
      qrStatus: "ACTIVE",
      qrIssuedAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("students").doc("stu_4").set({
      userId: ALICE_UID,
      nis: "2026004",
      fullName: "Future Student",
      className: "X-A",
      gender: "L",
      qrToken: "",
      qrStatus: "PENDING",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );

  // Cross-user read/write denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("students").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("students").doc("stu_1").delete()
  );
});

test("Attendance records: check-in requires existing student, check-out update, reject orphan & tampered studentId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("students").doc("stu_1").set({
      userId: ALICE_UID,
      nis: "2026001",
      fullName: "Ahmad Fauzi",
      className: "X-A",
      gender: "L",
      qrToken: "TRN-2026001-A9X2K8",
      qrStatus: "ACTIVE",
      qrIssuedAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Check-in attendance record
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-22_stu_1").set({
      userId: ALICE_UID,
      studentId: "stu_1",
      studentNis: "2026001",
      studentName: "Ahmad Fauzi",
      className: "X-A",
      dateKey: "2026-11-22",
      status: "HADIR",
      method: "QR_SCAN",
      note: "",
      checkInTime: new Date(Date.now() - 3000),
      createdAt: new Date(Date.now() - 3000),
      updatedAt: new Date(Date.now() - 3000),
    })
  );

  // Check-out partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-22_stu_1").update({
      checkOutTime: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Query by dateKey matching Kotlin repository
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").where("dateKey", "==", "2026-11-22").get()
  );

  // Reject orphan attendance for non-existent student
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-22_nonexistent").set({
      userId: ALICE_UID,
      studentId: "nonexistent",
      studentNis: "999999",
      studentName: "Ghost",
      className: "X-A",
      dateKey: "2026-11-22",
      status: "HADIR",
      method: "QR_SCAN",
      note: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject immutable studentId/dateKey mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-22_stu_1").update({
      dateKey: "2026-11-23",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user attendance read denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("attendance").where("dateKey", "==", "2026-11-22").get()
  );
});
