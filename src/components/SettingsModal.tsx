import React, { useState, useEffect } from 'react';
import { X, Settings, Phone, Globe, Youtube, MessageCircle, WifiOff } from 'lucide-react';
import { motion } from 'motion/react';

export interface FeatureFlags {
  enableCalling: boolean;
  enableAppOpening: boolean;
  enableMedia: boolean;
  enableWhatsApp: boolean;
  enableOfflineMode: boolean;
}

export const defaultFeatures: FeatureFlags = {
  enableCalling: true,
  enableAppOpening: true,
  enableMedia: true,
  enableWhatsApp: true,
  enableOfflineMode: true,
};

interface SettingsModalProps {
  onClose: () => void;
}

export function getFeatures(): FeatureFlags {
  if (typeof localStorage === 'undefined') return defaultFeatures;
  const saved = localStorage.getItem('ruhi_features');
  return saved ? { ...defaultFeatures, ...JSON.parse(saved) } : defaultFeatures;
}

export default function SettingsModal({ onClose }: SettingsModalProps) {
  const [features, setFeatures] = useState<FeatureFlags>(getFeatures());

  useEffect(() => {
    localStorage.setItem('ruhi_features', JSON.stringify(features));
  }, [features]);

  const toggleFeature = (key: keyof FeatureFlags) => {
    setFeatures(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const toggles = [
    { key: 'enableCalling', label: 'Voice Calling', icon: Phone, desc: 'Call contacts & numbers' },
    { key: 'enableAppOpening', label: 'App / Web Opening', icon: Globe, desc: 'Open apps & websites' },
    { key: 'enableMedia', label: 'Media Playback', icon: Youtube, desc: 'Play YouTube & Spotify' },
    { key: 'enableWhatsApp', label: 'WhatsApp Messages', icon: MessageCircle, desc: 'Send WhatsApp directly' },
    { key: 'enableOfflineMode', label: 'Offline Mode Fallback', icon: WifiOff, desc: 'Basic responses when offline' },
  ];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
    >
      <motion.div
        initial={{ scale: 0.95, y: 20 }}
        animate={{ scale: 1, y: 0 }}
        exit={{ scale: 0.95, y: 20 }}
        className="bg-[#111] border border-white/10 rounded-2xl p-6 w-full max-w-md shadow-2xl flex flex-col max-h-[80vh]"
      >
        <div className="flex justify-between items-center mb-6">
          <div className="flex items-center gap-2">
            <Settings size={20} className="text-violet-400" />
            <h2 className="text-xl font-medium text-white">Features</h2>
          </div>
          <button onClick={onClose} className="p-2 rounded-full hover:bg-white/10 text-white/70 transition-colors">
            <X size={20} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto pr-2 space-y-4">
          {toggles.map((t) => {
            const Icon = t.icon;
            const isEnabled = features[t.key as keyof FeatureFlags];
            return (
              <div key={t.key} className="flex items-center justify-between p-4 rounded-xl bg-white/5 border border-white/5 hover:bg-white/10 transition-colors">
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-black/50 text-white/70">
                    <Icon size={18} />
                  </div>
                  <div>
                    <div className="text-sm font-medium text-white">{t.label}</div>
                    <div className="text-xs text-white/50">{t.desc}</div>
                  </div>
                </div>
                <button
                  onClick={() => toggleFeature(t.key as keyof FeatureFlags)}
                  className={`w-12 h-6 rounded-full p-1 transition-colors ${isEnabled ? 'bg-violet-500' : 'bg-white/10'}`}
                >
                  <motion.div
                    animate={{ x: isEnabled ? 24 : 0 }}
                    className="w-4 h-4 rounded-full bg-white shadow-md"
                  />
                </button>
              </div>
            );
          })}
        </div>
      </motion.div>
    </motion.div>
  );
}
