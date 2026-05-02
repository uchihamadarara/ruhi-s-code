import React, { useState, useEffect } from 'react';
import { X, Plus, Phone, MessageSquare, Trash2 } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

export interface Contact {
  id: string;
  name: string;
  phone: string;
}

interface ContactsModalProps {
  onClose: () => void;
}

export default function ContactsModal({ onClose }: ContactsModalProps) {
  const [contacts, setContacts] = useState<Contact[]>(() => {
    const saved = localStorage.getItem('ruhi_contacts');
    return saved ? JSON.parse(saved) : [];
  });
  const [isAdding, setIsAdding] = useState(false);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');

  useEffect(() => {
    localStorage.setItem('ruhi_contacts', JSON.stringify(contacts));
  }, [contacts]);

  const handleAdd = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !phone.trim()) return;
    const newContact: Contact = { id: Date.now().toString(), name: name.trim(), phone: phone.trim() };
    setContacts(prev => [...prev, newContact]);
    setName('');
    setPhone('');
    setIsAdding(false);
  };

  const handleDelete = (id: string) => {
    setContacts(prev => prev.filter(c => c.id !== id));
  };

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
          <h2 className="text-xl font-medium text-white">Contacts</h2>
          <button onClick={onClose} className="p-2 rounded-full hover:bg-white/10 text-white/70 transition-colors">
            <X size={20} />
          </button>
        </div>

        {isAdding ? (
          <form onSubmit={handleAdd} className="flex flex-col gap-3 mb-6 bg-white/5 p-4 rounded-xl border border-white/10">
            <input
              type="text"
              placeholder="Name"
              value={name}
              onChange={e => setName(e.target.value)}
              className="bg-black/50 border border-white/10 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-violet-500/50"
              autoFocus
            />
            <input
              type="tel"
              placeholder="Phone Number"
              value={phone}
              onChange={e => setPhone(e.target.value)}
              className="bg-black/50 border border-white/10 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-violet-500/50"
            />
            <div className="flex gap-2 justify-end mt-2">
              <button type="button" onClick={() => setIsAdding(false)} className="px-3 py-1.5 text-sm rounded-lg hover:bg-white/10 text-white/70">
                Cancel
              </button>
              <button type="submit" disabled={!name.trim() || !phone.trim()} className="px-3 py-1.5 text-sm rounded-lg bg-violet-500 hover:bg-violet-600 disabled:opacity-50 text-white font-medium">
                Save
              </button>
            </div>
          </form>
        ) : (
          <button onClick={() => setIsAdding(true)} className="flex items-center justify-center gap-2 w-full py-3 rounded-xl border border-dashed border-white/20 text-white/60 hover:text-white/90 hover:bg-white/5 hover:border-white/30 transition-all mb-6">
            <Plus size={18} />
            <span className="text-sm">Add Contact</span>
          </button>
        )}

        <div className="flex-1 overflow-y-auto pr-2 space-y-2">
          {contacts.length === 0 && !isAdding && (
            <div className="text-center py-8 text-white/40 text-sm">No contacts saved yet.</div>
          )}
          {contacts.map(contact => (
            <div key={contact.id} className="flex items-center justify-between p-3 rounded-xl bg-white/5 border border-white/5 hover:bg-white/10 transition-colors group">
              <div className="flex-1 min-w-0">
                <div className="text-sm font-medium text-white truncate">{contact.name}</div>
                <div className="text-xs text-white/50 truncate">{contact.phone}</div>
              </div>
              <div className="flex items-center gap-1 opacity-100 md:opacity-0 group-hover:opacity-100 transition-opacity">
                <a href={`tel:${contact.phone}`} className="p-2 rounded-full hover:bg-green-500/20 text-green-400 transition-colors" title="Call">
                  <Phone size={16} />
                </a>
                <a href={`whatsapp://send?phone=${contact.phone}`} className="p-2 rounded-full hover:bg-blue-500/20 text-blue-400 transition-colors" title="Message">
                  <MessageSquare size={16} />
                </a>
                <button onClick={() => handleDelete(contact.id)} className="p-2 rounded-full hover:bg-red-500/20 text-red-400 transition-colors" title="Delete">
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))}
        </div>
      </motion.div>
    </motion.div>
  );
}
