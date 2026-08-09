import { useEffect, useState } from 'react';
import { Save, Eye, EyeOff } from 'lucide-react';
import { toast } from 'sonner';
import { obterEmpresa, atualizarEmpresa } from '../api/empresaApi';
import type { Empresa } from '../types/empresa';

type Tab = 'geral' | 'integracao' | 'notificacoes' | 'seguranca' | 'api';

const tabs: { key: Tab; label: string }[] = [
  { key: 'geral',         label: 'Geral' },
  { key: 'integracao',    label: 'Integração' },
  { key: 'notificacoes',  label: 'Notificações' },
  { key: 'seguranca',     label: 'Segurança' },
  { key: 'api',           label: 'API' },
];

function Toggle({ checked, onChange }: { checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <button
      type="button"
      onClick={() => onChange(!checked)}
      className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${checked ? 'bg-[#2563EB]' : 'bg-[#CBD5E1]'}`}
    >
      <span className={`inline-block h-3.5 w-3.5 transform rounded-full bg-white shadow transition-transform ${checked ? 'translate-x-4' : 'translate-x-0.5'}`} />
    </button>
  );
}

const campoClasse = 'w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all';

function BadgeTipo({ tipo }: { tipo: 'Facto' | 'Contexto' }) {
  const cor = tipo === 'Facto' ? 'bg-[#ECFDF5] text-[#059669]' : 'bg-[#F5F3FF] text-[#7C3AED]';
  return <span className={`text-[9px] px-1.5 py-0.5 rounded font-medium ${cor}`}>{tipo}</span>;
}

export function Configuracoes() {
  const [activeTab, setActiveTab] = useState<Tab>('geral');
  const [showApiKey, setShowApiKey] = useState(false);
  const [notifs, setNotifs] = useState({ prazos: true, lancamentos: true, erros: true, relatorios: false });
  const [seg, setSeg] = useState({ dual2fa: false, auditoria: true, aprovacaoDual: true });

  // Fase 2 — "Dados da Empresa" era 100% mock (só toast.success ao
  // "Guardar", sem ligação nenhuma ao backend). Passa a carregar/guardar
  // via GET/PUT /api/empresa (EmpresaController), incluindo os novos
  // campos de contexto contabilístico.
  const [empresa, setEmpresa] = useState<Empresa | null>(null);
  const [aCarregarEmpresa, setACarregarEmpresa] = useState(true);
  const [aGuardarEmpresa, setAGuardarEmpresa] = useState(false);

  useEffect(() => {
    obterEmpresa()
      .then(setEmpresa)
      .catch(err => toast.error(err instanceof Error ? err.message : 'Erro ao carregar dados da empresa'))
      .finally(() => setACarregarEmpresa(false));
  }, []);

  async function guardarEmpresa() {
    if (!empresa) return;
    setAGuardarEmpresa(true);
    try {
      const atualizada = await atualizarEmpresa(empresa);
      setEmpresa(atualizada);
      toast.success('Dados da empresa guardados');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Erro ao guardar dados da empresa');
    } finally {
      setAGuardarEmpresa(false);
    }
  }

  return (
    <div className="max-w-[900px] space-y-4">
      <div>
        <h1 className="text-[18px] font-semibold text-[#0F172A]">Configurações</h1>
        <p className="text-[13px] text-[#475569] mt-0.5">Sistema · Integração · Segurança</p>
      </div>

      {/* Tabs */}
      <div className="flex items-center border-b border-[#E2E8F0]">
        {tabs.map(tab => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2.5 text-[13px] font-medium transition-colors border-b-2 -mb-px ${
              activeTab === tab.key ? 'border-[#2563EB] text-[#2563EB]' : 'border-transparent text-[#475569] hover:text-[#0F172A]'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Geral */}
      {activeTab === 'geral' && (
        <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
          <div className="px-4 py-3 border-b border-[#E2E8F0]">
            <h2 className="text-[13px] font-semibold text-[#0F172A]">Dados da Empresa</h2>
          </div>

          {aCarregarEmpresa || !empresa ? (
            <div className="p-4 text-[13px] text-[#94A3B8]">A carregar…</div>
          ) : (
            <>
              <div className="p-4 grid grid-cols-1 lg:grid-cols-2 gap-4">
                <div>
                  <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Nome da Empresa</label>
                  <input type="text" placeholder="Empresa Lda." value={empresa.nome ?? ''}
                    onChange={e => setEmpresa({ ...empresa, nome: e.target.value })} className={campoClasse} />
                </div>
                <div>
                  <label className="block text-[12px] font-medium text-[#475569] mb-1.5">NIF</label>
                  <input type="text" placeholder="5000123456LA" value={empresa.nif ?? ''}
                    onChange={e => setEmpresa({ ...empresa, nif: e.target.value })} className={campoClasse}
                    style={{ fontFamily: 'JetBrains Mono, monospace' }} />
                </div>
                <div>
                  <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Email de Contacto</label>
                  <input type="text" placeholder="contabilidade@empresa.ao" value={empresa.email ?? ''}
                    onChange={e => setEmpresa({ ...empresa, email: e.target.value })} className={campoClasse} />
                </div>
                <div>
                  <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Telefone</label>
                  <input type="text" placeholder="+244 9XX XXX XXX" value={empresa.telefone ?? ''}
                    onChange={e => setEmpresa({ ...empresa, telefone: e.target.value })} className={campoClasse}
                    style={{ fontFamily: 'JetBrains Mono, monospace' }} />
                </div>
                <div className="lg:col-span-2">
                  <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Morada</label>
                  <input type="text" placeholder="Rua..., Luanda, Angola" value={empresa.endereco ?? ''}
                    onChange={e => setEmpresa({ ...empresa, endereco: e.target.value })} className={campoClasse} />
                </div>
              </div>

              {/* Fase 2 — contexto contabilístico da empresa: distinção
                  Facto/Contexto explícita na UI (nunca preenchido pela IA,
                  ver model/Empresa.java do backend). */}
              <div className="px-4 py-3 border-t border-[#E2E8F0]">
                <h3 className="text-[12px] font-semibold text-[#0F172A] mb-0.5">Contexto Contabilístico</h3>
                <p className="text-[11px] text-[#94A3B8] mb-3">
                  Usado para desambiguar classificações automáticas (ex: uma viatura é mercadoria para um stand automóvel, mas ativo para uma construtora). Preenchido só por um Administrador — nunca inferido pela IA.
                </p>
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                  <div>
                    <label className="flex items-center gap-1.5 text-[12px] font-medium text-[#475569] mb-1.5">
                      Atividade Económica <BadgeTipo tipo="Contexto" />
                    </label>
                    <input type="text" placeholder="Ex: Comércio a retalho de veículos automóveis"
                      value={empresa.atividadeEconomica ?? ''}
                      onChange={e => setEmpresa({ ...empresa, atividadeEconomica: e.target.value })} className={campoClasse} />
                  </div>
                  <div>
                    <label className="flex items-center gap-1.5 text-[12px] font-medium text-[#475569] mb-1.5">
                      Natureza do Negócio <BadgeTipo tipo="Contexto" />
                    </label>
                    <input type="text" placeholder="Ex: Revenda de mercadorias"
                      value={empresa.naturezaNegocio ?? ''}
                      onChange={e => setEmpresa({ ...empresa, naturezaNegocio: e.target.value })} className={campoClasse} />
                  </div>
                  <div>
                    <label className="flex items-center gap-1.5 text-[12px] font-medium text-[#475569] mb-1.5">
                      Moeda <BadgeTipo tipo="Facto" />
                    </label>
                    <input type="text" placeholder="AOA" value={empresa.moeda ?? ''}
                      onChange={e => setEmpresa({ ...empresa, moeda: e.target.value })} className={campoClasse} />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="flex items-center gap-1.5 text-[12px] font-medium text-[#475569] mb-1.5">
                        Início do Exercício <BadgeTipo tipo="Facto" />
                      </label>
                      <input type="date" value={empresa.exercicioAtualInicio ?? ''}
                        onChange={e => setEmpresa({ ...empresa, exercicioAtualInicio: e.target.value })} className={campoClasse} />
                    </div>
                    <div>
                      <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Fim do Exercício</label>
                      <input type="date" value={empresa.exercicioAtualFim ?? ''}
                        onChange={e => setEmpresa({ ...empresa, exercicioAtualFim: e.target.value })} className={campoClasse} />
                    </div>
                  </div>
                </div>
              </div>

              <div className="px-4 py-3 border-t border-[#E2E8F0]">
                <button onClick={guardarEmpresa} disabled={aGuardarEmpresa}
                  className="flex items-center gap-1.5 h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] disabled:opacity-60 text-white text-[13px] font-medium rounded-md transition-colors">
                  <Save style={{ width: 13, height: 13 }} /> {aGuardarEmpresa ? 'A guardar…' : 'Guardar'}
                </button>
              </div>
            </>
          )}
        </div>
      )}

      {/* Notificações */}
      {activeTab === 'notificacoes' && (
        <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
          <div className="px-4 py-3 border-b border-[#E2E8F0]">
            <h2 className="text-[13px] font-semibold text-[#0F172A]">Preferências de Alertas</h2>
          </div>
          <div className="p-4 space-y-0">
            {[
              { key: 'prazos', label: 'Alertas de prazos fiscais', desc: 'PD 71/25 — faturas com prazo excedido' },
              { key: 'lancamentos', label: 'Novos lançamentos IA', desc: 'Quando a IA cria sugestões pendentes' },
              { key: 'erros', label: 'Erros de processamento', desc: 'Falhas no OCR ou na categorização' },
              { key: 'relatorios', label: 'Relatórios mensais', desc: 'Envio automático no fecho do mês' },
            ].map(item => (
              <div key={item.key} className="flex items-center justify-between py-3 border-b border-[#F1F5F9] last:border-0">
                <div>
                  <p className="text-[13px] font-medium text-[#0F172A]">{item.label}</p>
                  <p className="text-[12px] text-[#475569]">{item.desc}</p>
                </div>
                <Toggle
                  checked={notifs[item.key as keyof typeof notifs]}
                  onChange={v => setNotifs(prev => ({ ...prev, [item.key]: v }))}
                />
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Segurança */}
      {activeTab === 'seguranca' && (
        <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
          <div className="px-4 py-3 border-b border-[#E2E8F0]">
            <h2 className="text-[13px] font-semibold text-[#0F172A]">Segurança e Auditoria</h2>
          </div>
          <div className="p-4 space-y-0">
            {[
              { key: 'dual2fa', label: 'Autenticação de dois factores', desc: 'Exigir 2FA para todos os utilizadores' },
              { key: 'auditoria', label: 'Registo de auditoria', desc: 'Registar todas as acções no sistema' },
              { key: 'aprovacaoDual', label: 'Aprovação dual', desc: 'Lançamentos acima de AOA 5.000.000 requerem aprovação dupla' },
            ].map(item => (
              <div key={item.key} className="flex items-center justify-between py-3 border-b border-[#F1F5F9] last:border-0">
                <div>
                  <p className="text-[13px] font-medium text-[#0F172A]">{item.label}</p>
                  <p className="text-[12px] text-[#475569]">{item.desc}</p>
                </div>
                <Toggle
                  checked={seg[item.key as keyof typeof seg]}
                  onChange={v => setSeg(prev => ({ ...prev, [item.key]: v }))}
                />
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Integração */}
      {activeTab === 'integracao' && (
        <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
          <div className="px-4 py-3 border-b border-[#E2E8F0]">
            <h2 className="text-[13px] font-semibold text-[#0F172A]">Spring Boot API</h2>
          </div>
          <div className="p-4 space-y-3">
            {[
              { label: 'URL da API', value: 'http://localhost:8080/api/v1', mono: true },
              { label: 'Base de Dados MySQL', value: 'jdbc:mysql://localhost:3306/conta_ia', mono: true },
            ].map(f => (
              <div key={f.label}>
                <label className="block text-[12px] font-medium text-[#475569] mb-1.5">{f.label}</label>
                <input
                  type="text"
                  defaultValue={f.value}
                  className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
                  style={f.mono ? { fontFamily: 'JetBrains Mono, monospace' } : {}}
                />
              </div>
            ))}
            <button onClick={() => toast.success('Configuração guardada')} className="flex items-center gap-1.5 h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-[13px] font-medium rounded-md transition-colors">
              <Save style={{ width: 13, height: 13 }} /> Guardar
            </button>
          </div>
        </div>
      )}

      {/* API */}
      {activeTab === 'api' && (
        <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
          <div className="px-4 py-3 border-b border-[#E2E8F0]">
            <h2 className="text-[13px] font-semibold text-[#0F172A]">Chave de API</h2>
          </div>
          <div className="p-4 space-y-4">
            <div>
              <label className="block text-[12px] font-medium text-[#475569] mb-1.5">API Key</label>
              <div className="relative">
                <input
                  type={showApiKey ? 'text' : 'password'}
                  defaultValue="sk-conta-ia-prod-abc123def456ghi789"
                  readOnly
                  className="w-full h-8 px-3 pr-9 text-[13px] border border-[#E2E8F0] rounded-md bg-[#F8FAFC] text-[#0F172A] focus:outline-none"
                  style={{ fontFamily: 'JetBrains Mono, monospace' }}
                />
                <button onClick={() => setShowApiKey(!showApiKey)} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[#94A3B8] hover:text-[#475569] transition-colors">
                  {showApiKey ? <EyeOff style={{ width: 14, height: 14 }} /> : <Eye style={{ width: 14, height: 14 }} />}
                </button>
              </div>
            </div>
            <div className="bg-[#F8FAFC] rounded-md p-3">
              <p className="text-[11px] font-medium text-[#475569] uppercase tracking-wide mb-2">Exemplo de uso</p>
              <pre className="text-[11px] text-[#475569] overflow-x-auto" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{`POST /api/v1/lancamentos
Authorization: Bearer {api_key}
Content-Type: application/json

{
  "data": "2026-07-08",
  "documento": "FT 001/2026",
  "contaDebito": "1.1.1",
  "contaCredito": "4.1.1",
  "valor": 1029000
}`}</pre>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
