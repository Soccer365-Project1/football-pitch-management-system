import React from 'react';
import type { RouteObject } from 'react-router';
import AdminLayout from '../layouts/AdminLayout';
import AdminDashboard from '../pages/admin/AdminDashboard';
import AdminTimeline from '../pages/admin/AdminTimeline';
import AdminBookings from '../pages/admin/AdminBookings';
import AdminTransactions from '../pages/admin/AdminTransactions';
import AdminUsers from '../pages/admin/AdminUsers';
import AdminPitches from '../pages/admin/AdminPitches';
import AdminTimeSlots from '../pages/admin/AdminTimeSlots';
import AdminPricing from '../pages/admin/AdminPricing';

export const adminRoutes: RouteObject[] = [
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      {
        index: true,
        element: <AdminDashboard />
      },
      {
        path: 'timeline',
        element: <AdminTimeline />
      },
      {
        path: 'bookings',
        element: <AdminBookings />
      },
      {
        path: 'transactions',
        element: <AdminTransactions />
      },
      {
        path: 'users',
        element: <AdminUsers />
      },
      {
        path: 'pitches',
        element: <AdminPitches />
      },
      {
        path: 'timeslots',
        element: <AdminTimeSlots />
      },
      {
        path: 'pricing',
        element: <AdminPricing />
      }
    ]
  }
];
