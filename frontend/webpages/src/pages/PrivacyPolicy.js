import React, { useEffect } from 'react';
import '../styles/PrivacyPolicy.css';

function PrivacyPolicy() {
  useEffect(() => { document.title = 'Privacy Policy | FairTix'; }, []);
  return (
    <div className="privacy-policy">
      <h2>Privacy Policy</h2>

      <section>
        <h3>What We Collect</h3>
        <p>
          When you create a FairTix account, we collect your email address and a
          securely hashed password. When you purchase tickets, we store your order
          history, ticket details, and seat selections.
        </p>
      </section>

      <section>
        <h3>How We Use Your Data</h3>
        <ul>
          <li>Authenticate you and manage your session</li>
          <li>Process ticket purchases and issue tickets</li>
          <li>Send transactional notifications you have opted into (order confirmations, ticket issuance)</li>
        </ul>
        <p>We will never sell your personal data to third parties.</p>
      </section>

      <section>
        <h3>Notification Preferences</h3>
        <p>
          You can manage which notifications you receive from the notification
          preferences section in your Dashboard. Marketing emails are strictly
          opt-in and will never be enabled by default.
        </p>
      </section>

      <section>
        <h3>Data Retention &amp; Deletion</h3>
        <p>
          You may delete your account at any time from your Dashboard. When you
          delete your account, your personal data is anonymized. Order and ticket
          records are retained in anonymized form for legal and record-keeping
          purposes.
        </p>
      </section>

      <section>
        <h3>Data Export</h3>
        <p>
          You can export all of your personal data from the Dashboard at any time.
          The export includes your profile information, orders, tickets, and
          notification preferences.
        </p>
      </section>

      <section>
        <h3>Cookies</h3>
        <p>
          FairTix uses HTTP-only cookies to maintain your session. These cookies
          cannot be read by JavaScript. A short-lived access token expires after
          15 minutes and is silently renewed using a refresh token that persists
          for up to 7 days. Both tokens are cleared when you log out.
        </p>
      </section>

      <section>
        <h3>Contact</h3>
        <p>
          For questions about this privacy policy, please reach out to the FairTix
          team.
        </p>
      </section>
    </div>
  );
}

export default PrivacyPolicy;
