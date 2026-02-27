<section class="border-b border-slate-200 bg-white">
    <div class="mx-auto grid w-full max-w-7xl gap-12 px-4 py-24 sm:px-6 md:grid-cols-2 md:items-center lg:gap-16 lg:px-8">
        <div>
            <p class="mb-4 inline-flex items-center rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-indigo-700">
                Plataforma SaaS de Gestão Escolar
            </p>
            <h1 class="text-3xl font-extrabold leading-tight tracking-tight text-slate-900 lg:text-5xl">
                Controle Total da Gestão Escolar
            </h1>
            <p class="mt-5 max-w-xl text-base leading-relaxed text-slate-600 sm:text-lg">
                Organize alunos, turmas, financeiro e operações administrativas em uma única plataforma estruturada.
            </p>

            <div class="mt-10 flex flex-col gap-4 sm:flex-row sm:items-center sm:gap-5">
                <a
                    href="#cta"
                    class="inline-flex w-full items-center justify-center rounded-2xl bg-indigo-600 px-6 py-3 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-indigo-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500/60 focus-visible:ring-offset-2 sm:w-auto sm:justify-start"
                >
                    Solicitar Demonstração
                </a>
                <a
                    href="{{ route('login') }}"
                    class="inline-flex w-full items-center justify-center rounded-2xl border border-slate-300 bg-white px-6 py-3 text-sm font-semibold text-slate-700 transition-all duration-200 hover:border-slate-400 hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500/40 focus-visible:ring-offset-2 sm:w-auto sm:justify-start"
                >
                    Acessar Plataforma
                </a>
            </div>
        </div>

        <div class="relative">
            <div class="pointer-events-none absolute -left-10 top-2 z-0 h-44 w-44 rounded-full bg-indigo-100/80 blur-3xl"></div>
            <div class="pointer-events-none absolute -right-10 bottom-2 z-0 h-44 w-44 rounded-full bg-teal-100/80 blur-3xl"></div>
            <div class="relative z-10 overflow-hidden rounded-2xl border border-slate-200 bg-white p-3 shadow-xl shadow-slate-300/50 sm:p-4">
                <img
                    src="{{ asset('images/alfaschool-dashboard-mockup-light.svg') }}"
                    alt="Mockup de dashboard administrativo do AlfaSchool"
                    class="h-auto w-full rotate-[1.5deg] rounded-xl shadow-lg ring-1 ring-slate-200/80"
                >
            </div>
        </div>
    </div>
</section>
