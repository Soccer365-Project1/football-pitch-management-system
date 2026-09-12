import React from 'react';
import type { RouteObject } from 'react-router-dom';

// Layouts
import CustomerLayout from '../layouts/CustomerLayout';

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
      {
        index: true,
        element: <Home />,
      },
      {
        path: 'login',
        element: <Login />,
      },
      {
        path: 'book-pitch',
        element: <BookPitch />,
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
        path: 'profile',
        element: <Profile />,
      },
      {
        path: 'my-bookings',
        element: <MyBookings />,
      },
      {
        path: 'checkout/:timeSlotId/:pitchId',
        element: <Checkout />,
      },
    ],
  },
];

