import { useState } from "react";

/**
 * Fetches address data from ViaCEP and merges into form state.
 * @param {function} setForm - React state setter for form
 * @param {function} [mapper] - Optional function(viaCepData, prev) => patch.
 *   Defaults to mapping into { logradouro, bairro, cidade, estado, complemento }.
 */
export function useCepLookup(setForm, mapper) {
  const [cepLoading, setCepLoading] = useState(false);

  const handleCepBlur = async (cep) => {
    const digits = (cep || "").replace(/\D/g, "");
    if (digits.length !== 8) return;
    setCepLoading(true);
    try {
      const res = await fetch(`https://viacep.com.br/ws/${digits}/json/`);
      const data = await res.json();
      if (!data.erro) {
        setForm((prev) => ({
          ...prev,
          ...(mapper
            ? mapper(data, prev)
            : {
                logradouro: data.logradouro || prev.logradouro,
                bairro: data.bairro || prev.bairro,
                cidade: data.localidade || prev.cidade,
                estado: data.uf || prev.estado,
                complemento: data.complemento || prev.complemento,
              }),
        }));
      }
    } catch {
      // silently ignore ViaCEP errors
    } finally {
      setCepLoading(false);
    }
  };

  return { cepLoading, handleCepBlur };
}
