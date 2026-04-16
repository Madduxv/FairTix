// Google reCAPTCHA v2 integration for React frontend
import React, { useEffect, useRef } from 'react';

const RECAPTCHA_SITE_KEY =
  process.env.REACT_APP_RECAPTCHA_SITE_KEY;
const WIDGET_SCALE = 1.03;
const WIDGET_WIDTH = 304;
const WIDGET_HEIGHT = 78;

// Wait for grecaptcha before attempting to render the widget.
function waitForGrecaptchaRender(cb) {
  if (window.grecaptcha && typeof window.grecaptcha.render === 'function') {
    cb();
  } else {
    setTimeout(() => waitForGrecaptchaRender(cb), 100);
  }
}

const Recaptcha = React.forwardRef(function Recaptcha({ onChange }, ref) {
  const recaptchaRef = useRef(null);
  const widgetIdRef = useRef(null);
  const containerId = useRef(`recaptcha-container-${Math.random().toString(36).substring(2, 10)}`);

  React.useImperativeHandle(ref, () => ({
    reset: () => {
      if (window.grecaptcha && widgetIdRef.current !== null) {
        window.grecaptcha.reset(widgetIdRef.current);
      }
    }
  }), []);

  useEffect(() => {
    if (!RECAPTCHA_SITE_KEY) {
      return undefined;
    }

    let isMounted = true;

    function renderRecaptchaOnce() {
      if (!isMounted) return;
      if (window.grecaptcha && widgetIdRef.current === null) {
        widgetIdRef.current = window.grecaptcha.render(containerId.current, {
          sitekey: RECAPTCHA_SITE_KEY,
          callback: (token) => {
            onChange(token);
          },
          'expired-callback': () => {
            onChange('');
          },
          theme: 'light',
        });
      }
    }

    if (!window.grecaptcha) {
      const scriptSrc = 'https://www.google.com/recaptcha/api.js?render=explicit';
      const existingScript = document.querySelector(`script[src="${scriptSrc}"]`);

      if (existingScript) {
        waitForGrecaptchaRender(renderRecaptchaOnce);
      } else {
        const script = document.createElement('script');
        script.src = scriptSrc;
        script.async = true;
        script.defer = true;
        document.body.appendChild(script);
        script.onload = () => {
          waitForGrecaptchaRender(renderRecaptchaOnce);
        };
      }

      return () => {
        isMounted = false;
      };
    }

    waitForGrecaptchaRender(renderRecaptchaOnce);
    return () => {
      isMounted = false;
    };
  }, [onChange]);

  useEffect(() => {
    return () => {
      if (window.grecaptcha && widgetIdRef.current !== null) {
        // Keep widgetIdRef intact to avoid duplicate render in React StrictMode's
        // development-only double invocation of effects.
        window.grecaptcha.reset(widgetIdRef.current);
      }
    };
  }, []);

  if (!RECAPTCHA_SITE_KEY) {
    return (
      <div style={{ margin: '16px 0', color: '#b00020', fontSize: '0.9rem' }}>
        Missing REACT_APP_RECAPTCHA_SITE_KEY in frontend/webpages/.env
      </div>
    );
  }

  return (
    <div
      ref={recaptchaRef}
      style={{
        margin: '16px 0',
        width: `${Math.round(WIDGET_WIDTH * WIDGET_SCALE)}px`,
        height: `${Math.round(WIDGET_HEIGHT * WIDGET_SCALE)}px`,
        overflow: 'hidden',
      }}
    >
      <div
        id={containerId.current}
        style={{
          transform: `scale(${WIDGET_SCALE})`,
          transformOrigin: '0 0',
          filter: 'contrast(1.06)',
        }}
      />
    </div>
  );
});

export default Recaptcha;