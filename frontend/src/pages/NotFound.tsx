import React from 'react';
import { Link } from 'react-router-dom';
import { Home, ArrowLeft } from 'lucide-react';

const NotFound: React.FC = () => {
  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4 space-y-6">
      <div className="text-7xl font-extrabold text-emerald-600 dark:text-emerald-400 tracking-wider animate-bounce">
        404
      </div>
      <div className="space-y-2">
        <h2 className="text-2xl md:text-3xl font-bold text-slate-900 dark:text-slate-100">
          Trang Bạn Tìm Kiếm Không Tồn Tại!
        </h2>
        <p className="text-slate-500 dark:text-slate-400 text-sm max-w-md mx-auto">
          Đường dẫn có thể đã bị thay đổi hoặc không có sẵn trong hệ thống Soccer365 FPMS.
        </p>
      </div>
      <div className="flex gap-3">
        <Link 
          to="/" 
          className="px-5 py-2.5 rounded-xl bg-emerald-600 text-white font-bold text-sm hover:bg-emerald-700 transition-all shadow-md shadow-emerald-600/20 flex items-center gap-2"
        >
          <Home size={18} /> Về Trang Chủ
        </Link>
      </div>
    </div>
  );
};

export default NotFound;
