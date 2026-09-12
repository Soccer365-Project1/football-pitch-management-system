import React from 'react';
import { Link } from 'react-router-dom';
import { Construction, ArrowLeft, Clock, Sparkles } from 'lucide-react';

interface UnderDevelopmentProps {
  title?: string;
  moduleName?: string;
  description?: string;
  backLink?: string;
  backText?: string;
}

const UnderDevelopment: React.FC<UnderDevelopmentProps> = ({
  title = 'Chức năng đang được phát triển',
  moduleName = 'Mô-đun Sprint Sau',
  description = 'Trang này hiện chưa khả dụng và sẽ được hoàn thiện cùng hệ thống Backend APIs ở các Sprint tiếp theo.',
  backLink = '/',
  backText = 'Quay về Trang Chủ'
}) => {
  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center text-center p-6 space-y-5 animate-fade-in">
      <div className="relative">
        <div className="w-20 h-20 rounded-3xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shadow-inner">
          <Construction size={40} />
        </div>
        <span className="absolute -top-1 -right-1 flex h-4 w-4">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
          <span className="relative inline-flex rounded-full h-4 w-4 bg-emerald-500"></span>
        </span>
      </div>

      <div className="space-y-2 max-w-md">
        <span className="px-3 py-1 rounded-full bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 text-xs font-bold tracking-wide uppercase inline-flex items-center gap-1.5">
          <Sparkles size={12} className="text-emerald-500" /> {moduleName}
        </span>
        <h2 className="text-2xl font-extrabold text-slate-900 dark:text-slate-100">{title}</h2>
        <p className="text-slate-500 dark:text-slate-400 text-sm leading-relaxed">{description}</p>
      </div>

      <div className="pt-2">
        <Link 
          to={backLink}
          className="px-5 py-2.5 rounded-xl bg-emerald-600 text-white font-bold text-sm hover:bg-emerald-700 transition-all shadow-md shadow-emerald-600/20 inline-flex items-center gap-2"
        >
          <ArrowLeft size={16} /> {backText}
        </Link>
      </div>
    </div>
  );
};

export default UnderDevelopment;
