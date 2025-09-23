'use client';

import { useState } from 'react';
import { messagePollingService } from '@/services/messagePollingService';

interface PollingToggleProps {
  className?: string;
}

export default function PollingToggle({ className = '' }: PollingToggleProps) {
  const [isEnabled, setIsEnabled] = useState(messagePollingService.isPollingOn());

  const handleToggle = () => {
    const newState = !isEnabled;
    setIsEnabled(newState);
    messagePollingService.setPollingEnabled(newState);
  };

  return (
    <button
      onClick={handleToggle}
      className={`flex items-center gap-2 px-3 py-1 rounded-lg text-xs transition-colors ${
        isEnabled
          ? 'bg-green-100 text-green-700 hover:bg-green-200'
          : 'bg-red-100 text-red-700 hover:bg-red-200'
      } ${className}`}
      title={isEnabled ? 'Disable polling' : 'Enable polling'}
    >
      <div className={`w-2 h-2 rounded-full ${isEnabled ? 'bg-green-500' : 'bg-red-500'}`} />
      <span>{isEnabled ? 'Polling ON' : 'Polling OFF'}</span>
    </button>
  );
}