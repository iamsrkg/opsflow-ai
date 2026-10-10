import React, { useState, useEffect } from 'react';
import { RunbookSearchResult } from '../services/types';
import { ApiService } from '../services/api';
import { BookOpen, Search, Layers, Cpu } from 'lucide-react';

export const RunbooksView: React.FC = () => {
  const [query, setQuery] = useState('payment sync kafka lag');
  const [results, setResults] = useState<RunbookSearchResult[]>([]);

  const handleSearch = async () => {
    try {
      setResults(await ApiService.searchRunbooks(query));
    } catch {
      // the reason is shown in the banner at the top of the page
    }
  };

  useEffect(() => {
    handleSearch();
  }, []);

  return (
    <div className="content-body">
      <div className="table-container" style={{ padding: '1.25rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
          <div style={{ fontWeight: 700, fontSize: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <BookOpen size={18} color="#22d3ee" />
            <span>Runbook search</span>
          </div>
          <div className="search-box" style={{ width: '380px' }}>
            <Search size={16} color="#9ca3af" />
            <input
              type="text"
              placeholder="Describe the problem, e.g. payment deducted but order pending"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            />
            <button className="btn-secondary" style={{ padding: '0.25rem 0.65rem', fontSize: '0.75rem' }} onClick={handleSearch}>
              Search
            </button>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {results.map((item) => (
            <div key={item.chunkId} style={{ background: 'var(--bg-dark)', border: '1px solid var(--border-color)', borderRadius: '10px', padding: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <h3 style={{ fontSize: '1rem', color: '#a5b4fc', fontWeight: 700 }}>{item.documentTitle}</h3>
                <span className="badge badge-booking" style={{ fontSize: '0.72rem' }}>
                  <Cpu size={12} /> Similarity {Math.round(item.score * 100)}%
                </span>
              </div>
              <div style={{ fontSize: '0.88rem', color: 'var(--text-main)', lineHeight: 1.6 }}>
                {item.snippet}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
