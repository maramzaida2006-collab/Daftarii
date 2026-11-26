# Project Proposal: Daftari

## 1. Project Overview

Daftari is an Android application that replaces the paper debt notebook used by supermarket and grocery owners. The owner adds each customer, records every purchase on credit and every payment, and the app automatically keeps the customer's total balance up to date. Its main purpose is to give shop owners an accurate, secure and searchable debt record that they can export as a PDF statement and share with the customer.

## 2. Problem Statement

Most neighbourhood supermarkets sell on credit and track debts in a paper notebook. The owner writes each purchase by hand and later crosses out or subtracts payments, then has to add up every page manually to know how much a customer owes.

This causes real problems: totals are often miscalculated, pages get lost, torn or wet, handwriting becomes hard to read, and there is no clear history to show a customer when they dispute an amount. Finding one customer's records among hundreds of entries wastes time at the counter, and if the notebook is lost, the money owed is effectively lost with it.

The impact is lost income, wasted time, and damaged trust between the owner and the customers. Daftari solves this by storing every transaction digitally, calculating balances automatically, keeping deleted entries in a recoverable archive, and producing a clear PDF statement for each customer.

## 3. Target Users

**Primary User Type 1: Shop Owner**
- Description: Supermarket / grocery owner who sells to regular customers on credit.
- Goals: Record debts and payments quickly, always know each customer's exact balance, and have proof of every transaction.
- Pain Points: Paper notebook is slow to search, totals are calculated by hand with errors, and records can be lost or damaged.

**Primary User Type 2: Small Business Owner**
- Description: Owners of other small shops that sell on credit (bakery, pharmacy, vegetable shop, minimarket).
- Goals: A simple, secure tool that works on their phone without training or extra hardware.
- Pain Points: Existing accounting software is too complex or expensive for a small shop; they need only debt tracking.

**Secondary User Type 3: Customer (debtor)**
- Description: Regular customers who buy on credit and pay later.
- Goals: Know exactly how much they owe and see a clear history of purchases and payments.
- Pain Points: Disputes over amounts because they cannot see the owner's handwritten notebook.

## 4. Core Features (Must-Have for MVP)

1. **Accounts, Email Verification & Guest Mode**
   - Description: Short onboarding, then sign up with a real email (activation link must be verified before login), login and password reset. A guest mode lets the owner try the app without an account and later convert it to a real account without losing any data.
   - User Benefit: Easy first try, while protecting the owner's financial data with a verified, recoverable account.

2. **Biometric (Fingerprint) Login**
   - Description: After the first login, the owner can unlock the app with the phone's fingerprint instead of typing the password.
   - User Benefit: Fast access at the counter while keeping the debt records private and secure.

3. **Customer Management**
   - Description: Add, edit, search and archive customers (name, phone number) and set one payment due date per customer. The list shows each customer's total balance and status (on time, due soon, overdue).
   - User Benefit: Find any customer in seconds and see who owes what at a glance.

4. **Debt & Payment Transactions**
   - Description: Record a new debt (purchase on credit) or a payment, with amount, date and note. Payments are deducted from the customer's total balance, which updates automatically. Transactions can be edited, and every edit is saved in an edit history (old value, new value, date).
   - User Benefit: No manual calculation or math mistakes, and a transparent history the customer can trust.

5. **Transaction Archive & Restore**
   - Description: When the owner deletes a transaction it is moved to an archive instead of being erased. From the archive it can be restored or permanently deleted, and the balance is recalculated.
   - User Benefit: Protects against accidental deletion and keeps a trustworthy history.

6. **Export Customer Statement to PDF**
   - Description: Generate a PDF of a customer's full transaction history and current balance, ready to share (e.g., WhatsApp) or print.
   - User Benefit: Gives the customer clear proof of their account and reduces disputes.

## 5. Additional Features (Nice-to-Have)

1. **Dark Mode** - A full dark theme for all screens, switchable from the profile screen or following the phone's system setting.
2. **Push Notifications & Payment Reminders** - Phone notifications when a customer's due date is near or has passed, even when the app is closed, with a one-tap WhatsApp reminder to the customer.
3. **Full Reports Dashboard** - Collection rate, new debts vs. payments over the last 6 months in charts, overdue customers and top debtors.

*These will be implemented if time permits.*

## 6. Technical Stack

**Frontend:**
- Language & IDE: Java – Android Studio (the team already knows Java; Android Studio is the official Android IDE; we will improve the Product Owner's initial screens)
- UI & Architecture: XML Layouts, Material Components, MVVM (ViewModel + LiveData) – keeps UI separate from business logic and makes the code easier to test

**Backend:**
- Authentication: Firebase Authentication – built-in email verification links, password reset and anonymous (guest) sign-in; a guest account is upgraded with `linkWithCredential` so its data is kept

**Database:**
- Cloud Firestore – NoSQL cloud database with offline support and real-time sync; each owner sees only their own data (security rules)

**Device Services:**
- AndroidX Biometric (BiometricPrompt for fingerprint login)
- WorkManager (daily due-date check that shows local phone notifications without needing a server)
- Android PdfDocument API (generates customer statements on the device to share or print)

**Development Tools:**
- Version Control: Git / GitHub (feature branches and pull requests reviewed by a teammate)
- Project Management: GitHub Projects (product backlog, sprint board and user stories)
- Testing: JUnit (balance and archive/restore logic) + Espresso (key UI flows)
- CI/CD (optional): GitHub Actions (Gradle build and unit tests run automatically on every push / pull request)

## 7. System Architecture

**Basic flow:** Shop Owner → Android App (Activities / Fragments) → ViewModel → Repository → Firebase (Authentication + Cloud Firestore)

- **Android App (Java, MVVM):** screens for login/registration, customer list, customer details & transactions, archive, and settings. ViewModels hold screen state, and Repository classes contain the business logic (balance calculation, archive/restore) and all Firebase access.
- **Firebase Authentication:** creates the account, sends the email activation link, checks that the email is verified before allowing login, and handles password reset.
- **Cloud Firestore:** stores all data under the owner: `users/{ownerId}/customers/{customerId}` and `users/{ownerId}/transactions/{transactionId}` (each transaction holds its customerId). Balances are updated atomically with `FieldValue.increment` in the same batch as the transaction, and amounts are stored as whole agorot (long) to avoid rounding errors. Each transaction has type (debt or payment), amount, date, note, an `isArchived` flag and an `editHistory` list; each customer has one `dueDate`. Security rules allow each owner to access only their own data.
- **On-device services:** BiometricPrompt verifies the fingerprint to unlock the app; the PdfDocument API builds the customer statement on the phone, which is then shared or printed.

## 8. Project Timeline

**Sprint 1 (Week 3-4):** Project setup (GitHub board, Firebase, new data structure and security rules, app theme from the approved design), splash and onboarding, registration with email verification, login, password reset and guest mode.

**Sprint 2 (Week 5-6):** PIN and fingerprint lock screen, guest-to-account upgrade; customer management: add, edit, search, filters, due dates and customer status.

**Sprint 3 (Week 7-8):** Debt and payment transactions with automatic balance calculation; customer transaction history screen.

**Sprint 4 (Week 9-10):** Transaction archive & restore with balance recalculation; export customer statement to PDF and share.

**Sprint 5 (Week 11-12):** Nice-to-have features: push notifications for due dates, full reports dashboard and dark mode; UI polish and bug fixing.

**Sprint 6 (Week 13-14):** Final testing and presentation prep.

## 9. Success Criteria

The project will be considered successful if:
- [ ] All core features are implemented and working
- [ ] Users can complete primary workflows without errors
- [ ] Code is well-documented and maintainable
- [ ] Test coverage is at least 70%
- [ ] Application is responsive and user-friendly
- [ ] Security best practices are followed
- [ ] Project-specific: A customer's balance always equals total debts minus total payments, including after archiving or restoring transactions
- [ ] Project-specific: Email accounts must be verified before login, guest data is kept when upgrading to an account, and a PDF statement can be generated for any customer

## 10. Risks and Mitigation

**Risk 1:** Team is new to Firebase and Android architecture (MVVM) – Likelihood: M, Impact: H
**Mitigation:** Spend part of Sprint 1 on tutorials and a small prototype; pair programming; share useful resources in the team repo.

**Risk 2:** Wrong balance calculations (data integrity) – Likelihood: M, Impact: H
**Mitigation:** Calculate the balance from the transactions in one service function, use Firestore transactions for updates, and cover it with unit tests.

**Risk 3:** Scope creep – new feature requests from the client/PO – Likelihood: M, Impact: M
**Mitigation:** Freeze the MVP backlog; new ideas go to the nice-to-have list for Sprints 5–6 and are prioritized by the Product Owner.

**Risk 4:** Biometric or device compatibility problems – Likelihood: M, Impact: M
**Mitigation:** Always keep password login as a fallback; test on several Android devices and emulators.

## 11. Team Roles and Responsibilities

**Maram Zaida - Product Owner + Developer (Data & Logic):**
- Manages product backlog, defines priorities, accepts completed work
- Develops: Firestore data model and rules, transactions, balance, edit history and archive logic

**Sondos Abu Skhail - Scrum Master + Developer (Auth & Quality):**
- Facilitates Scrum ceremonies, removes blockers, tracks progress
- Develops: authentication, guest mode, PIN/fingerprint lock, push notifications, tests and CI

**Marwa Zaida - Developer (UI & Features):**
- Implements the approved design: app theme, all screens, navigation, customer screens, PDF statement, reports dashboard and dark mode

*Note: All members contribute code in every sprint; these are primary focuses.*

## 12. Deliverables

- Working Android application
- Source code on GitHub
- Documentation
- Test suite
- Presentation slides
- Demo video

---

**Approved by Team:**
- Maram Zaida - 29 November 2026
- Sondos Abu Skhail - 29 November 2026
- Marwa Zaida - 29 November 2026

**Instructor Approval:** ________________ Date: _______
