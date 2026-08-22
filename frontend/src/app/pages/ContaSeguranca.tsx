import { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { ShieldCheck, ShieldOff, Copy, Check } from 'lucide-react';
import { toast } from 'sonner';
import {
  obterEstado2FA,
  iniciarSetup2FA,
  confirmarSetup2FA,
  desativar2FA,
} from '../api/twoFactorApi';

type Fase = 'inicial' | 'setup' | 'recuperacao';

const campoClasse = 'w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all';

// Página pessoal de segurança de conta ("/minha-conta") — substitui o
// toggle "Autenticação de dois factores" que existia em Configuracoes.tsx
// (tab Segurança), que era só estado local sem qualquer ligação ao
// backend. Ativar/desativar 2FA é uma acção sobre a própria conta, por
// isso fica aqui em vez de numa página de configurações do sistema — ver
// TwoFactorController.java (sem @PreAuthorize, qualquer perfil gere o
// próprio 2FA).
export function ContaSeguranca() {
  const [carregando, setCarregando] = useState(true);
  const [ativo, setAtivo] = useState(false);
  const [fase, setFase] = useState<Fase>('inicial');

  const [setupSecret, setSetupSecret] = useState('');
  const [qrDataUrl, setQrDataUrl] = useState('');
  const [codigo, setCodigo] = useState('');
  const [aConfirmar, setAConfirmar] = useState(false);
  const [aIniciarSetup, setAIniciarSetup] = useState(false);

  const [codigosRecuperacao, setCodigosRecuperacao] = useState<string[]>([]);
  const [codigosCopiados, setCodigosCopiados] = useState(false);

  const [mostrarDesativar, setMostrarDesativar] = useState(false);
  const [passwordDesativar, setPasswordDesativar] = useState('');
  const [aDesativar, setADesativar] = useState(false);

  useEffect(() => {
    obterEstado2FA()
      .then(status => setAtivo(status.ativo))
      .catch(err => toast.error(err instanceof Error ? err.message : 'Erro ao carregar estado do 2FA'))
      .finally(() => setCarregando(false));
  }, []);

  async function handleIniciarSetup() {
    setAIniciarSetup(true);
    try {
      const setup = await iniciarSetup2FA();
      const qr = await QRCode.toDataURL(setup.otpauthUrl);
      setSetupSecret(setup.secret);
      setQrDataUrl(qr);
      setCodigo('');
      setFase('setup');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Erro ao iniciar configuração do 2FA');
    } finally {
      setAIniciarSetup(false);
    }
  }

  async function handleConfirmar(e: React.FormEvent) {
    e.preventDefault();
    if (!codigo) {
      toast.error('Introduza o código gerado pela app de autenticação');
      return;
    }
    setAConfirmar(true);
    try {
      const resp = await confirmarSetup2FA(codigo);
      setCodigosRecuperacao(resp.codigosRecuperacao);
      setAtivo(true);
      setFase('recuperacao');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Código inválido');
    } finally {
      setAConfirmar(false);
    }
  }

  function handleConcluirRecuperacao() {
    setFase('inicial');
    setCodigosRecuperacao([]);
    setCodigosCopiados(false);
    toast.success('Autenticação de dois factores ativada');
  }

  async function handleCopiarCodigos() {
    await navigator.clipboard.writeText(codigosRecuperacao.join('\n'));
    setCodigosCopiados(true);
    toast.success('Códigos copiados');
  }

  async function handleDesativar(e: React.FormEvent) {
    e.preventDefault();
    if (!passwordDesativar) {
      toast.error('Introduza a sua senha para confirmar');
      return;
    }
    setADesativar(true);
    try {
      await desativar2FA(passwordDesativar);
      setAtivo(false);
      setMostrarDesativar(false);
      setPasswordDesativar('');
      toast.success('Autenticação de dois factores desativada');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Senha incorreta');
    } finally {
      setADesativar(false);
    }
  }

  return (
    <div className="max-w-[640px] space-y-4">
      <div>
        <h1 className="text-[18px] font-semibold text-[#0F172A]">A minha conta</h1>
        <p className="text-[13px] text-[#475569] mt-0.5">Segurança de acesso</p>
      </div>

      <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
        <div className="px-4 py-3 border-b border-[#E2E8F0]">
          <h2 className="text-[13px] font-semibold text-[#0F172A]">Autenticação de dois factores</h2>
        </div>

        <div className="p-4">
          {carregando ? (
            <p className="text-[13px] text-[#94A3B8]">A carregar…</p>
          ) : fase === 'setup' ? (
            <form onSubmit={handleConfirmar} className="space-y-4">
              <p className="text-[13px] text-[#475569]">
                Digitalize o código QR com a sua app de autenticação (Google Authenticator, Authy, etc.) ou introduza a chave manualmente.
              </p>
              <div className="flex flex-col sm:flex-row gap-4 items-start">
                {qrDataUrl && (
                  <img src={qrDataUrl} alt="Código QR para configurar 2FA" className="w-[160px] h-[160px] border border-[#E2E8F0] rounded-md flex-shrink-0" />
                )}
                <div className="flex-1 space-y-3 min-w-0">
                  <div>
                    <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Chave manual</label>
                    <input type="text" readOnly value={setupSecret} className={campoClasse} style={{ fontFamily: 'JetBrains Mono, monospace' }} />
                  </div>
                  <div>
                    <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Código de confirmação</label>
                    <input
                      type="text"
                      inputMode="numeric"
                      value={codigo}
                      onChange={e => setCodigo(e.target.value)}
                      placeholder="123456"
                      className={campoClasse}
                      style={{ fontFamily: 'JetBrains Mono, monospace', letterSpacing: '2px' }}
                    />
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="submit"
                  disabled={aConfirmar}
                  className="flex items-center gap-1.5 h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] disabled:opacity-60 text-white text-[13px] font-medium rounded-md transition-colors"
                >
                  {aConfirmar ? 'A confirmar…' : 'Confirmar e ativar'}
                </button>
                <button
                  type="button"
                  onClick={() => setFase('inicial')}
                  className="h-8 px-3 text-[13px] text-[#475569] hover:text-[#0F172A] transition-colors"
                >
                  Cancelar
                </button>
              </div>
            </form>
          ) : fase === 'recuperacao' ? (
            <div className="space-y-4">
              <div className="bg-[#FFFBEB] border border-[#FDE68A] rounded-md p-3">
                <p className="text-[13px] font-medium text-[#92400E]">Guarde estes códigos de recuperação</p>
                <p className="text-[12px] text-[#92400E] mt-0.5">
                  Cada código só pode ser usado uma vez, para entrar caso perca acesso à app de autenticação. Não voltam a ser mostrados.
                </p>
              </div>
              <div className="bg-[#F8FAFC] border border-[#E2E8F0] rounded-md p-3 grid grid-cols-2 gap-2">
                {codigosRecuperacao.map(c => (
                  <span key={c} className="text-[13px] text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{c}</span>
                ))}
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={handleCopiarCodigos}
                  className="flex items-center gap-1.5 h-8 px-3 border border-[#E2E8F0] hover:bg-[#F8FAFC] text-[#475569] text-[13px] font-medium rounded-md transition-colors"
                >
                  {codigosCopiados ? <Check style={{ width: 13, height: 13 }} /> : <Copy style={{ width: 13, height: 13 }} />}
                  {codigosCopiados ? 'Copiado' : 'Copiar códigos'}
                </button>
                <button
                  type="button"
                  onClick={handleConcluirRecuperacao}
                  className="h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-[13px] font-medium rounded-md transition-colors"
                >
                  Concluir
                </button>
              </div>
            </div>
          ) : ativo ? (
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-[#059669]">
                <ShieldCheck style={{ width: 16, height: 16 }} />
                <p className="text-[13px] font-medium">A autenticação de dois factores está ativa nesta conta.</p>
              </div>
              {!mostrarDesativar ? (
                <button
                  type="button"
                  onClick={() => setMostrarDesativar(true)}
                  className="h-8 px-3 border border-[#FCA5A5] text-[#DC2626] hover:bg-[#FEF2F2] text-[13px] font-medium rounded-md transition-colors"
                >
                  Desativar 2FA
                </button>
              ) : (
                <form onSubmit={handleDesativar} className="space-y-3 max-w-[320px]">
                  <div>
                    <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Confirme a sua senha para desativar</label>
                    <input
                      type="password"
                      value={passwordDesativar}
                      onChange={e => setPasswordDesativar(e.target.value)}
                      placeholder="••••••••"
                      className={campoClasse}
                    />
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      type="submit"
                      disabled={aDesativar}
                      className="h-8 px-3 bg-[#DC2626] hover:bg-[#B91C1C] disabled:opacity-60 text-white text-[13px] font-medium rounded-md transition-colors"
                    >
                      {aDesativar ? 'A desativar…' : 'Confirmar desativação'}
                    </button>
                    <button
                      type="button"
                      onClick={() => { setMostrarDesativar(false); setPasswordDesativar(''); }}
                      className="h-8 px-3 text-[13px] text-[#475569] hover:text-[#0F172A] transition-colors"
                    >
                      Cancelar
                    </button>
                  </div>
                </form>
              )}
            </div>
          ) : (
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-[#94A3B8]">
                <ShieldOff style={{ width: 16, height: 16 }} />
                <p className="text-[13px]">A autenticação de dois factores está desativada.</p>
              </div>
              <p className="text-[12px] text-[#475569]">
                Ao ativar, será pedido um código gerado por uma app de autenticação (Google Authenticator, Authy, etc.) sempre que iniciar sessão.
              </p>
              <button
                type="button"
                onClick={handleIniciarSetup}
                disabled={aIniciarSetup}
                className="h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] disabled:opacity-60 text-white text-[13px] font-medium rounded-md transition-colors"
              >
                {aIniciarSetup ? 'A preparar…' : 'Ativar 2FA'}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
