import React, { useEffect, useRef } from 'react';
import './Modal.css';

const FOCUSABLE = 'button:not(:disabled), [href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])';

function Modal({ isOpen = true, onClose, title, titleId, size = 'md', children }) {
  const dialogRef = useRef(null);

  useEffect(() => {
    if (!isOpen) return;

    const handleEsc = (e) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleEsc);

    const focusable = dialogRef.current?.querySelectorAll(FOCUSABLE);
    const alreadyFocused = dialogRef.current?.contains(document.activeElement);
    if (!alreadyFocused && focusable?.length) {
      setTimeout(() => focusable[0].focus(), 0);
    }

    return () => document.removeEventListener('keydown', handleEsc);
  }, [isOpen, onClose]);

  useEffect(() => {
    if (!isOpen || !dialogRef.current) return;

    const handleTab = (e) => {
      if (e.key !== 'Tab') return;
      const nodes = Array.from(dialogRef.current.querySelectorAll(FOCUSABLE));
      if (!nodes.length) return;
      const first = nodes[0];
      const last = nodes[nodes.length - 1];
      if (e.shiftKey) {
        if (document.activeElement === first) { e.preventDefault(); last.focus(); }
      } else {
        if (document.activeElement === last) { e.preventDefault(); first.focus(); }
      }
    };

    document.addEventListener('keydown', handleTab);
    return () => document.removeEventListener('keydown', handleTab);
  }, [isOpen]);

  if (!isOpen) return null;

  const labelId = titleId || 'modal-title';

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        ref={dialogRef}
        className={`modal modal--${size}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={labelId}
        onClick={(e) => e.stopPropagation()}
      >
        {title && <h3 id={labelId} className="modal-title">{title}</h3>}
        {children}
      </div>
    </div>
  );
}

export default Modal;
