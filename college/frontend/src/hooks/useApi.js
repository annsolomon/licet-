import { useCallback, useEffect, useState } from 'react';

/** Loads data when the page opens (and again on reload()). Returns { data, error, loading, reload }. */
export function useApi(loader, deps = []) {
  const [state, setState] = useState({ data: null, error: null, loading: true });
  const run = useCallback(() => {
    setState((s) => ({ ...s, loading: true, error: null }));
    loader()
      .then((data) => setState({ data, error: null, loading: false }))
      .catch((error) => setState({ data: null, error, loading: false }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  useEffect(() => { run(); }, [run]);
  return { ...state, reload: run };
}

/** Runs an action (button click) and keeps its result / error. */
export function useAction() {
  const [state, setState] = useState({ result: null, error: null, busy: false });
  const run = useCallback(async (fn) => {
    setState({ result: null, error: null, busy: true });
    try {
      const result = await fn();
      setState({ result, error: null, busy: false });
      return result;
    } catch (error) {
      setState({ result: null, error, busy: false });
      return undefined;
    }
  }, []);
  return { ...state, run, clear: () => setState({ result: null, error: null, busy: false }) };
}
