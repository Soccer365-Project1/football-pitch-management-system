import type { RouteObject } from 'react-router-dom';

// Layouts & Route Guard
import CustomerLayout from '../layouts/CustomerLayout';
import ProtectedRoute from '../components/common/ProtectedRoute';

// Pages
import Home from '../pages/Home';
import Login from '../pages/Login';
import BookPitch from '../pages/BookPitch';
import Register from '../pages/Register';
import ForgotPassword from '../pages/ForgotPassword';
import Profile from '../pages/Profile';
import MyBookings from '../pages/MyBookings';
import Checkout from '../pages/Checkout';

export const customerRoutes: RouteObject[] = [
  {
    path: '/',
    element: <CustomerLayout />,
    children: [
      // 1. CÁC TRANG CÔNG KHAI (Mở tự do để tiện phát triển và kiểm thử)
      {
        index: true,
        element: <Home />,
      },
      {
        path: 'login',
        element: <Login />,
      },
      {
        path: 'register',
        element: <Register />,
      },
      {
        path: 'forgot-password',
        element: <ForgotPassword />,
      },
      {
        path: 'book-pitch',
        element: <BookPitch />,
      },
      {
        path: 'my-bookings',
        element: <MyBookings />,
      },
      {
        path: 'checkout/:timeSlotId/:pitchId',
        element: <Checkout />,
      },

      // 2. CÁC TRANG YÊU CẦU ĐĂNG NHẬP (Bảo vệ bằng ProtectedRoute)
      {
        element: <ProtectedRoute />,
        children: [
          {
            path: 'profile',
            element: <Profile />,
          },
        ],
      },
    ],
  },
];
