import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { useQueryClient } from '@tanstack/react-query';

const getSocketUrl = () => {
  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL;
  }
  // Avoid mixed content if loaded via HTTPS
  if (typeof window !== 'undefined' && window.location.protocol === 'https:') {
    return `https://${window.location.hostname}:8080`;
  }
  return 'http://localhost:8080';
};

const getBrokerUrl = () => {
  const socketUrl = getSocketUrl();
  const wsProto = socketUrl.startsWith('https') ? 'wss:' : 'ws:';
  const host = socketUrl.replace(/^https?:\/\//, '');
  return `${wsProto}//${host}/ws`;
};

export const useScheduleWebSocket = (_selectedDate?: string) => {
  const queryClient = useQueryClient();
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    let client: Client | null = null;

    try {
      client = new Client({
        brokerURL: getBrokerUrl(),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        debug: () => {
          // Disable verbose STOMP debug traces to keep browser console clean
        },
        onConnect: () => {
          setIsConnected(true);

          // Subscribe to schedule topic for real-time booking updates
          client?.subscribe('/topic/schedule', () => {
            // Invalidate cache to refetch latest status
            queryClient.invalidateQueries({ queryKey: ['scheduleGrid'] });
            queryClient.invalidateQueries({ queryKey: ['pitches'] });
            queryClient.invalidateQueries({ queryKey: ['myBookings'] });
            queryClient.invalidateQueries({ queryKey: ['adminTimeline'] });
          });
        },
        onStompError: (frame) => {
          // Log as info/warning to prevent red console errors during server reconnects
          console.warn('STOMP connection notice:', frame.headers?.message || 'Server notice');
        },
        onWebSocketClose: () => {
          setIsConnected(false);
        },
        onDisconnect: () => {
          setIsConnected(false);
        },
      });

      client.activate();
    } catch {
      // Graceful fallback if WebSocket is blocked or unsupported in current environment
      setIsConnected(false);
    }

    return () => {
      try {
        if (client && client.active) {
          client.deactivate();
        }
      } catch {
        // Safe teardown
      }
    };
  }, [queryClient]);

  return { isConnected };
};
