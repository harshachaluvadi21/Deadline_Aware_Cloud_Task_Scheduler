import React, { useState, useEffect } from 'react';
import {
  analyzeSimulation,
  callGemini,
  hasGeminiApiKey,
} from '../services/gemini';
import { MetricsDto, TaskResultDto } from '../types/simulation';

interface GeminiAiAnalystProps {
  isCompare: boolean;
  baselineMetrics?: MetricsDto;
  proposedMetrics?: MetricsDto;
  singleMetrics?: MetricsDto;
  singleAlgorithm?: string;
  tasksCount: number;
  baselineTasks?: TaskResultDto[];
  proposedTasks?: TaskResultDto[];
}

export const GeminiAiAnalyst: React.FC<GeminiAiAnalystProps> = ({
  isCompare,
  baselineMetrics,
  proposedMetrics,
  singleMetrics,
  singleAlgorithm,
  tasksCount,
  baselineTasks,
  proposedTasks,
}) => {
  const [hasKey, setHasKey] = useState<boolean>(false);
  const [analysisText, setAnalysisText] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Viva Q&A prompt state
  const [customQuestion, setCustomQuestion] = useState<string>('');
  const [qaAnswer, setQaAnswer] = useState<string | null>(null);
  const [isQaLoading, setIsQaLoading] = useState<boolean>(false);

  useEffect(() => {
    setHasKey(hasGeminiApiKey());
  }, []);

  const handleRunAnalysis = async () => {
    if (!hasKey) {
      setError('Gemini API key is not configured in .env.local.');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const result = await analyzeSimulation({
        isCompare,
        baselineMetrics,
        proposedMetrics,
        singleMetrics,
        singleAlgorithm,
        tasksCount,
        baselineTasks,
        proposedTasks,
      });
      setAnalysisText(result);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to analyze with Gemini.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleAskVivaQuestion = async (presetQuestion?: string) => {
    const q = presetQuestion || customQuestion;
    if (!q.trim()) return;

    if (!hasKey) {
      setError('Gemini API key is not configured in .env.local.');
      return;
    }

    setIsQaLoading(true);
    setError(null);

    const contextPrompt = `Context: Cloud Task Scheduling Simulation (4 VMs, 1000 MIPS each).
Metrics:
- Baseline: Makespan=${baselineMetrics?.makespan}s, AvgWait=${baselineMetrics?.averageWaitingTime}s, MissRate=${baselineMetrics?.deadlineMissRate}%
- Deadline-Aware: Makespan=${proposedMetrics?.makespan}s, AvgWait=${proposedMetrics?.averageWaitingTime}s, MissRate=${proposedMetrics?.deadlineMissRate}%

Question from Examiner / Student:
"${q}"

Answer in 2-3 concise, technically precise sentences suitable for answering an external college viva examiner:`;

    try {
      const response = await callGemini(contextPrompt);
      setQaAnswer(response);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to get answer from Gemini.');
    } finally {
      setIsQaLoading(false);
    }
  };

  // Helper to format markdown headers and bold lines cleanly
  const renderFormattedMarkdown = (content: string) => {
    const lines = content.split('\n');
    return lines.map((line, idx) => {
      if (line.startsWith('### ')) {
        return (
          <h4 key={idx} style={{ color: 'var(--cyan)', marginTop: 18, marginBottom: 8, fontSize: '1.05rem', fontWeight: 600 }}>
            {line.replace('### ', '')}
          </h4>
        );
      }
      if (line.startsWith('## ')) {
        return (
          <h3 key={idx} style={{ color: 'var(--text-bright)', marginTop: 20, marginBottom: 10, fontSize: '1.15rem', fontWeight: 700 }}>
            {line.replace('## ', '')}
          </h3>
        );
      }
      if (line.startsWith('* ') || line.startsWith('- ')) {
        const text = line.replace(/^[\*\-]\s+/, '');
        return (
          <div key={idx} style={{ display: 'flex', gap: 8, margin: '4px 0 4px 12px', lineHeight: 1.5, color: 'var(--text-secondary)' }}>
            <span style={{ color: 'var(--cyan)' }}>•</span>
            <span dangerouslySetInnerHTML={{ __html: text.replace(/\*\*(.*?)\*\*/g, '<strong style="color:var(--text-bright)">$1</strong>') }} />
          </div>
        );
      }
      if (line.trim().length === 0) {
        return <div key={idx} style={{ height: 6 }} />;
      }
      return (
        <p key={idx} style={{ margin: '6px 0', lineHeight: 1.6, color: 'var(--text-secondary)' }}
          dangerouslySetInnerHTML={{ __html: line.replace(/\*\*(.*?)\*\*/g, '<strong style="color:var(--text-bright)">$1</strong>') }}
        />
      );
    });
  };

  return (
    <div className="card" style={{
      marginBottom: 24,
      background: 'var(--bg-card)',
      border: '1px solid var(--border)',
      boxShadow: 'var(--shadow-sm)',
      borderRadius: 'var(--r-lg)',
      overflow: 'hidden',
    }}>
      {/* Header bar */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: 16,
        paddingBottom: 16,
        borderBottom: '1px solid var(--border)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{
            width: 38,
            height: 38,
            borderRadius: 'var(--r-md)',
            background: 'linear-gradient(135deg, rgba(14, 165, 233, 0.15), rgba(99, 102, 241, 0.2))',
            border: '1px solid rgba(56, 189, 248, 0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--cyan)',
            flexShrink: 0,
          }}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
            </svg>
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 2 }}>
              <span style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-bright)' }}>
                Gemini Cloud AI Analyst
              </span>
              <span className="badge badge-cyan" style={{ fontSize: '0.68rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Google Gemini
              </span>
            </div>
            <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Architectural insights on CloudSim execution metrics and viva preparation.
            </p>
          </div>
        </div>

        <div>
          <button
            className="btn btn-primary btn-sm"
            onClick={handleRunAnalysis}
            disabled={isLoading}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 8,
              padding: '8px 16px',
              fontSize: '0.85rem',
              fontWeight: 600,
            }}
          >
            {isLoading ? (
              <>
                <span className="spinner" style={{ width: 14, height: 14 }} />
                Analyzing Metrics…
              </>
            ) : (
              <>
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3Z"/>
                </svg>
                Run AI Analysis
              </>
            )}
          </button>
        </div>
      </div>

      {/* Error alert */}
      {error && (
        <div className="alert alert-error" style={{ marginTop: 14 }}>
          <span>{error}</span>
        </div>
      )}

      {/* Analysis Result Output */}
      {analysisText && (
        <div style={{
          marginTop: 18,
          padding: '18px 20px',
          background: 'var(--bg-card-subtle, rgba(255, 255, 255, 0.02))',
          borderRadius: 'var(--r-md)',
          border: '1px solid var(--border)',
          lineHeight: 1.6,
        }}>
          {renderFormattedMarkdown(analysisText)}
        </div>
      )}

      {/* Interactive Viva Q&A Assistant */}
      <div style={{
        marginTop: 20,
        paddingTop: 16,
        borderTop: '1px solid var(--border)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M22 10v6M2 10l10-5 10 5-10 5z"/>
            <path d="M6 12v5c3 3 9 3 12 0v-5"/>
          </svg>
          <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-bright)' }}>
            Examiner Viva & Defense Q&A
          </span>
        </div>

        {/* Preset quick question chips */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 14 }}>
          {[
            'Why did Average Waiting Time improve?',
            'Why is there a 1-task miss trade-off on this 10-task set?',
            'How does the Fibonacci Heap optimize this?',
            'How would you scale this to 1000 tasks in production?',
          ].map((preset, idx) => (
            <button
              key={idx}
              className="btn btn-ghost btn-xs"
              onClick={() => handleAskVivaQuestion(preset)}
              disabled={isQaLoading}
              style={{
                fontSize: '0.75rem',
                padding: '5px 12px',
                borderRadius: 'var(--r-full, 9999px)',
                background: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid var(--border)',
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                color: 'var(--text-secondary)',
              }}
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ opacity: 0.7 }}>
                <circle cx="12" cy="12" r="10"/>
                <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              {preset}
            </button>
          ))}
        </div>

        {/* Custom question bar */}
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <input
            type="text"
            className="input-field"
            placeholder="Ask anything about these results (e.g., What is Makespan in this context?)..."
            value={customQuestion}
            onChange={e => setCustomQuestion(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleAskVivaQuestion()}
            style={{ flex: 1, fontSize: '0.82rem', padding: '9px 14px' }}
          />
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => handleAskVivaQuestion()}
            disabled={isQaLoading || !customQuestion.trim()}
            style={{
              padding: '9px 18px',
              fontSize: '0.82rem',
              fontWeight: 600,
              flexShrink: 0,
            }}
          >
            {isQaLoading ? 'Thinking…' : 'Ask'}
          </button>
        </div>

        {/* Q&A Answer display */}
        {qaAnswer && (
          <div style={{
            marginTop: 14,
            padding: '14px 18px',
            background: 'var(--bg-card-subtle, rgba(255, 255, 255, 0.02))',
            borderLeft: '3px solid var(--cyan)',
            borderRadius: '0 8px 8px 0',
            fontSize: '0.85rem',
            lineHeight: 1.55,
            color: 'var(--text-primary)',
          }}>
            <div style={{ fontWeight: 600, color: 'var(--cyan)', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="m9 12 2 2 4-4"/>
                <circle cx="12" cy="12" r="10"/>
              </svg>
              Examiner Answer:
            </div>
            <p style={{ margin: 0 }}>{qaAnswer}</p>
          </div>
        )}
      </div>
    </div>
  );
};
