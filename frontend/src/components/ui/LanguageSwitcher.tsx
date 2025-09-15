'use client';

import { Globe } from 'lucide-react';
import { useLanguage, Language } from '@/contexts/LanguageContext';

export default function LanguageSwitcher() {
  const { language, setLanguage, t } = useLanguage();

  const handleLanguageChange = (newLanguage: Language) => {
    setLanguage(newLanguage);
  };

  return (
    <div className="flex items-center gap-2">
      <Globe size={16} className="text-surface-500" />
      <select
        value={language}
        onChange={(e) => handleLanguageChange(e.target.value as Language)}
        className="text-sm bg-transparent border-none text-surface-700 focus:outline-none cursor-pointer"
      >
        <option value="en">{t('settings.language.en')}</option>
        <option value="zh-CN">{t('settings.language.zh-CN')}</option>
      </select>
    </div>
  );
}