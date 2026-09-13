import { createBrowserRouter } from 'react-router-dom';
import { customerRoutes } from './customer.routes';
import { adminRoutes } from './admin.routes';
import NotFound from '../pages/NotFound';

export const router = createBrowserRouter([
  ...customerRoutes,
  ...adminRoutes,
  {
    path: '*',
    element: <NotFound />,
  },
]);
