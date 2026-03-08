import { Outlet } from 'react-router-dom';
import AppSidebar from '../components/AppSidebar';

const AppLayout = () => {
  return (
    <div className="min-h-screen bg-background">
      <AppSidebar />
      <main className="ml-[240px] p-8 min-h-screen">
        <Outlet />
      </main>
    </div>
  );
};

export default AppLayout;
