import React, { useState, useEffect } from 'react';

interface CountdownTimerProps {
  holdExpiresAt: string; // ISO date string from backend
  onExpire?: () => void;
}

const CountdownTimer: React.FC<CountdownTimerProps> = ({ holdExpiresAt, onExpire }) => {
  const [timeLeft, setTimeLeft] = useState<number>(0);

  useEffect(() => {
    const calculateTimeLeft = () => {
      const difference = new Date(holdExpiresAt).getTime() - new Date().getTime();
      if (difference <= 0) {
        setTimeLeft(0);
        if (onExpire) {
          onExpire();
        }
      } else {
        setTimeLeft(Math.floor(difference / 1000));
      }
    };

    calculateTimeLeft();
    const timer = setInterval(calculateTimeLeft, 1000);

    return () => clearInterval(timer);
  }, [holdExpiresAt, onExpire]);

  if (timeLeft <= 0) {
    return <span className="text-danger font-semibold">Đã quá hạn (00:00)</span>;
  }

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;

  return (
    <div className="flex items-center gap-2">
      <span className="text-warning font-medium">Thời gian giữ chỗ còn lại:</span>
      <span className="font-bold text-lg" style={{ color: 'var(--color-primary)' }}>
        {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
      </span>
    </div>
  );
};

export default CountdownTimer;
