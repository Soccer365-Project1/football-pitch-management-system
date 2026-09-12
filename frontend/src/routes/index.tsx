import { createBrowserRouter } from 'react-router-dom';
import { customerRoutes } from './customer.routes';
import NotFound from '../pages/NotFound';
import React from 'react';

export const router = createBrowserRouter([
  ...customerRoutes,
  {
    path: '*',
    element: <NotFound />,
  },
]);
