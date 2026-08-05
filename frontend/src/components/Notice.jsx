import React from 'react';

export default function Notice({ message, onDismiss }) {
  if (!message) return null;
  return (
    <div className="notice">
      {message}
      <button onClick={onDismiss} aria-label="Dismiss notification">×</button>
    </div>
  );
}
