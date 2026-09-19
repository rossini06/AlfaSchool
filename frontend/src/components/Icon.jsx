import {
  Activity,
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Award,
  BarChart2,
  BarChart3,
  Bell,
  BookMarked,
  BookOpen,
  Building2,
  Calendar,
  CalendarCheck,
  CalendarClock,
  Check,
  CheckCircle,
  CheckSquare,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ClipboardCheck,
  ClipboardList,
  Clock,
  Cpu,
  DollarSign,
  DoorOpen,
  Download,
  Edit,
  Edit2,
  Edit3,
  Eye,
  FileEdit,
  FileSpreadsheet,
  FileText,
  Filter,
  Gavel,
  Globe,
  GraduationCap,
  History,
  Home,
  Info,
  Key,
  LayoutDashboard,
  LayoutGrid,
  Loader,
  Lock,
  LogOut,
  Map,
  Menu,
  Monitor,
  Moon,
  Network,
  NotebookPen,
  Plus,
  Printer,
  RefreshCw,
  Save,
  ScanFace,
  School,
  Search,
  Settings,
  Shield,
  ShieldCheck,
  Sun,
  Tag,
  Timer,
  Trash,
  Trash2,
  TrendingUp,
  Unlock,
  Upload,
  UserCheck,
  UserCog,
  UserPlus,
  Users,
  Users2,
  Wifi,
  WifiOff,
  X,
  XCircle,
  XSquare,
  Zap,
} from "lucide-react";

/**
 * Ponte entre o nome do icone escrito nas telas e o desenho do lucide.
 *
 * <h2>O que havia antes</h2>
 * Um dicionario com 80 `<path d="...">` escritos a mao. Quando a tela pedia
 * um nome que nao estava la', o componente devolvia um <span> vazio do
 * tamanho certo — sem desenho, sem log, sem aviso. O resultado eram
 * **14 dos 38 itens do menu com icone invisivel**, e com a barra lateral
 * recolhida sobrava um item de menu em branco e clicavel. O botao
 * "Permissoes" da tela de Usuarios era um retangulo vazio, e o triangulo de
 * alerta do modal de exclusao nao aparecia dentro do circulo vermelho.
 *
 * <h2>Por que a lista e' explicita</h2>
 * `import * as lucide` traria os 1.744 icones da biblioteca para o bundle.
 * A lista nomeada permite ao Vite descartar o que nao se usa. O preco e'
 * lembrar de acrescentar o nome aqui ao usar um icone novo — e por isso um
 * nome desconhecido agora AVISA no console em desenvolvimento, em vez de
 * sumir em silencio como antes.
 */
const ICONES = {
  Activity,
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Award,
  BarChart2,
  BarChart3,
  Bell,
  BookMarked,
  BookOpen,
  Building2,
  Calendar,
  CalendarCheck,
  CalendarClock,
  Check,
  CheckCircle,
  CheckSquare,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ClipboardCheck,
  ClipboardList,
  Clock,
  Cpu,
  DollarSign,
  DoorOpen,
  Download,
  Edit,
  Edit2,
  Edit3,
  Eye,
  FileEdit,
  FileSpreadsheet,
  FileText,
  Filter,
  Gavel,
  Globe,
  GraduationCap,
  History,
  Home,
  Info,
  Key,
  LayoutDashboard,
  LayoutGrid,
  Loader,
  Lock,
  LogOut,
  Map,
  Menu,
  Monitor,
  Moon,
  Network,
  NotebookPen,
  Plus,
  Printer,
  RefreshCw,
  Save,
  ScanFace,
  School,
  Search,
  Settings,
  Shield,
  ShieldCheck,
  Sun,
  Tag,
  Timer,
  Trash,
  Trash2,
  TrendingUp,
  Unlock,
  Upload,
  UserCheck,
  UserCog,
  UserPlus,
  Users,
  Users2,
  Wifi,
  WifiOff,
  X,
  XCircle,
  XSquare,
  Zap,

  // UserClock nao existe no lucide sob nome nenhum. A tela e' "Jornadas dos
  // Alunos", que associa aluno a uma janela de horario recorrente:
  // CalendarClock carrega as duas ideias. Clock puro ja' e' o icone de
  // Permanencia e criaria ambiguidade no mesmo menu.
  UserClock: CalendarClock,
};

export function Icon({ name, size = 16, className = "", style = {}, strokeWidth = 2, ...resto }) {
  const Desenho = ICONES[name];

  if (!Desenho) {
    if (import.meta.env.DEV) {
      console.warn(
        `[Icon] "${name}" nao esta registrado em src/components/Icon.jsx. ` +
          `Acrescente o nome ao import do lucide-react — senao ele fica invisivel na tela.`
      );
    }
    return null;
  }

  return (
    <Desenho
      size={size}
      strokeWidth={strokeWidth}
      className={className}
      style={{ display: "inline-block", verticalAlign: "-0.125em", flexShrink: 0, ...style }}
      aria-hidden="true"
      {...resto}
    />
  );
}

export default Icon;
