import type { RouteObject } from 'react-router';
import ProtectedRoute from '../components/common/ProtectedRoute';
import AdminLayout from '../layouts/AdminLayout';
import AdminDashboard from '../pages/admin/AdminDashboard';
import AdminTimeline from '../pages/admin/AdminTimeline';
import AdminBookings from '../pages/admin/AdminBookings';
import AdminTransactions from '../pages/admin/AdminTransactions';
import AdminUsers from '../pages/admin/AdminUsers';
import AdminPitches from '../pages/admin/AdminPitches';
import AdminTimeSlots from '../pages/admin/AdminTimeSlots';
import AdminPricing from '../pages/admin/AdminPricing';
import AdminProfile from '../pages/admin/AdminProfile';

export const adminRoutes: RouteObject[] = [
  {
    path: '/admin',
    element: <ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_STAFF']} />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          {
            index: true,
            element: (
              <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                <AdminDashboard />
              </ProtectedRoute>
            )
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
            element: (
              <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                <AdminPitches />
              </ProtectedRoute>
            )
          },
          {
            path: 'timeslots',
            element: (
              <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                <AdminTimeSlots />
              </ProtectedRoute>
            )
          },
          {
            path: 'pricing',
            element: (
              <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                <AdminPricing />
              </ProtectedRoute>
            )
          },
          {
            path: 'profile',
            element: <AdminProfile />
          }
        ]
      }
    ]
  }
];
