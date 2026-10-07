import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useQueryClient } from '@tanstack/react-query';

const SOCKET_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const useScheduleWebSocket = (selectedDate: string) => {
  const queryClient = useQueryClient();
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${SOCKET_URL}/ws`),
      onConnect: () => {
        setIsConnected(true);
        console.log('Connected to WebSocket');

        // Subscribe to schedule topic
        client.subscribe('/topic/schedule', (message) => {
          console.log('Received WebSocket message:', message.body);

          // Invalidate the schedule grid cache to refetch
          queryClient.invalidateQueries({ queryKey: ['scheduleGrid'] });
          queryClient.invalidateQueries({ queryKey: ['pitches'] });
        });
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      },
      onDisconnect: () => {
        setIsConnected(false);
        console.log('Disconnected from WebSocket');
      }
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [queryClient, selectedDate]);

  return { isConnected };
};
