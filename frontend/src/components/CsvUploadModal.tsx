import React, { useState, useRef } from 'react';
import { TaskDto, WorkloadValidationResponse } from '../types/simulation';
import { ApiError, simulationApi } from '../services/api';

interface CsvUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onWorkloadLoaded: (tasks: TaskDto[]) => void;
}

export const CsvUploadModal: React.FC<CsvUploadModalProps> = ({
  isOpen,
  onClose,
  onWorkloadLoaded
}) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isValidating, setIsValidating] = useState<boolean>(false);
  const [validationResult, setValidationResult] = useState<WorkloadValidationResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [errorDetails, setErrorDetails] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
      setValidationResult(null);
      setErrorMessage(null);
      setErrorDetails([]);
    }
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setSelectedFile(e.dataTransfer.files[0]);
      setValidationResult(null);
      setErrorMessage(null);
      setErrorDetails([]);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
  };

  const handleValidate = async () => {
    if (!selectedFile) return;
    setIsValidating(true);
    setErrorMessage(null);
    setErrorDetails([]);
    setValidationResult(null);

    try {
      const result = await simulationApi.validateWorkloadCsv(selectedFile);
      setValidationResult(result);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message);
        setErrorDetails(err.messages);
      } else if (err instanceof Error) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage('Failed to validate CSV workload.');
      }
    } finally {
      setIsValidating(false);
    }
  };

  const handleApply = () => {
    if (validationResult && validationResult.valid && validationResult.tasks.length > 0) {
      onWorkloadLoaded(validationResult.tasks);
      onClose();
    }
  };

  const handleClear = () => {
    setSelectedFile(null);
    setValidationResult(null);
    setErrorMessage(null);
    setErrorDetails([]);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(0, 0, 0, 0.75)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
      padding: '20px'
    }}>
      <div style={{
        background: 'var(--surface-color, #1e293b)',
        border: '1px solid var(--border-color, #334155)',
        borderRadius: '12px',
        width: '100%',
        maxWidth: '680px',
        maxHeight: '90vh',
        display: 'flex',
        flexDirection: 'column',
        boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.5)'
      }}>
        {/* Modal Header */}
        <div style={{
          padding: '16px 24px',
          borderBottom: '1px solid var(--border-color, #334155)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <h3 style={{ margin: 0, fontSize: '1.2rem', color: 'var(--text-color, #f8fafc)' }}>
            Import Workload from CSV
          </h3>
          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              color: '#94a3b8',
              fontSize: '1.4rem',
              cursor: 'pointer',
              lineHeight: 1
            }}
          >
            &times;
          </button>
        </div>

        {/* Modal Body */}
        <div style={{ padding: '20px 24px', overflowY: 'auto', flex: 1 }}>
          <p style={{ margin: '0 0 16px', fontSize: '0.88rem', color: '#94a3b8' }}>
            Upload a CSV file containing workload tasks. Expected columns:{' '}
            <code style={{ background: '#0f172a', padding: '2px 6px', borderRadius: '4px' }}>
              taskId,priority,arrivalTime,executionTime,deadline
            </code>
          </p>

          {/* Dropzone */}
          <div
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onClick={() => fileInputRef.current?.click()}
            style={{
              border: '2px dashed var(--border-color, #475569)',
              borderRadius: '8px',
              padding: '28px 16px',
              textAlign: 'center',
              cursor: 'pointer',
              background: selectedFile ? 'rgba(56, 189, 248, 0.05)' : '#0f172a',
              transition: 'border-color 0.2s'
            }}
          >
            <input
              type="file"
              ref={fileInputRef}
              accept=".csv,text/csv"
              style={{ display: 'none' }}
              onChange={handleFileChange}
            />
            {selectedFile ? (
              <div>
                <span style={{ fontSize: '1.8rem', display: 'block', marginBottom: '8px' }}>📄</span>
                <strong style={{ color: '#38bdf8' }}>{selectedFile.name}</strong>
                <p style={{ margin: '4px 0 0', fontSize: '0.8rem', color: '#94a3b8' }}>
                  {(selectedFile.size / 1024).toFixed(1)} KB — Click or drop to replace
                </p>
              </div>
            ) : (
              <div>
                <span style={{ fontSize: '1.8rem', display: 'block', marginBottom: '8px' }}>📁</span>
                <span style={{ color: '#e2e8f0', fontWeight: 500 }}>
                  Click to choose a CSV file or drag and drop here
                </span>
                <p style={{ margin: '4px 0 0', fontSize: '0.8rem', color: '#64748b' }}>
                  Supports UTF-8 comma-separated task definitions
                </p>
              </div>
            )}
          </div>

          {/* Validation error display */}
          {errorMessage && (
            <div className="alert-error" style={{ marginTop: '16px' }}>
              <strong>{errorMessage}</strong>
              {errorDetails.length > 0 && (
                <ul style={{ margin: '8px 0 0', paddingLeft: '20px', fontSize: '0.85rem' }}>
                  {errorDetails.map((err, idx) => (
                    <li key={idx}>{err}</li>
                  ))}
                </ul>
              )}
            </div>
          )}

          {/* Successful validation preview */}
          {validationResult && validationResult.valid && (
            <div style={{ marginTop: '16px' }}>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: '10px'
              }}>
                <span style={{ color: '#4ade80', fontWeight: 600, fontSize: '0.95rem' }}>
                  ✓ Validation Passed ({validationResult.taskCount} tasks detected)
                </span>
              </div>

              {/* Preview Table */}
              <div style={{ maxHeight: '200px', overflowY: 'auto', border: '1px solid #334155', borderRadius: '6px' }}>
                <table style={{ width: '100%', fontSize: '0.82rem', borderCollapse: 'collapse', textAlign: 'left' }}>
                  <thead>
                    <tr style={{ background: '#0f172a', borderBottom: '1px solid #334155' }}>
                      <th style={{ padding: '8px' }}>Task ID</th>
                      <th style={{ padding: '8px' }}>Priority</th>
                      <th style={{ padding: '8px' }}>Arrival (s)</th>
                      <th style={{ padding: '8px' }}>Exec Time (s)</th>
                      <th style={{ padding: '8px' }}>Deadline (s)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {validationResult.tasks.slice(0, 10).map((t) => (
                      <tr key={t.taskId} style={{ borderBottom: '1px solid #1e293b' }}>
                        <td style={{ padding: '6px 8px' }}>{t.taskId}</td>
                        <td style={{ padding: '6px 8px' }}>{t.priority}</td>
                        <td style={{ padding: '6px 8px' }}>{t.arrivalTime.toFixed(2)}</td>
                        <td style={{ padding: '6px 8px' }}>{t.executionTime.toFixed(2)}</td>
                        <td style={{ padding: '6px 8px' }}>{t.deadline.toFixed(2)}</td>
                      </tr>
                    ))}
                    {validationResult.tasks.length > 10 && (
                      <tr>
                        <td colSpan={5} style={{ padding: '6px 8px', textAlign: 'center', color: '#94a3b8', fontStyle: 'italic' }}>
                          ... and {validationResult.tasks.length - 10} more tasks
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div style={{
          padding: '16px 24px',
          borderTop: '1px solid var(--border-color, #334155)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <div>
            {selectedFile && (
              <button
                className="btn btn-secondary"
                style={{ padding: '6px 12px', fontSize: '0.85rem' }}
                onClick={handleClear}
                disabled={isValidating}
              >
                Clear File
              </button>
            )}
          </div>
          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              className="btn btn-secondary"
              onClick={onClose}
              disabled={isValidating}
            >
              Cancel
            </button>

            {validationResult?.valid ? (
              <button
                className="btn btn-primary"
                onClick={handleApply}
              >
                Load into Simulator
              </button>
            ) : (
              <button
                className="btn btn-primary"
                onClick={handleValidate}
                disabled={!selectedFile || isValidating}
              >
                {isValidating ? 'Validating CSV...' : 'Validate CSV'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
