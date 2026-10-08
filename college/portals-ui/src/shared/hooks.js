import { useCallback, useEffect, useState } from 'react';

/** Loads on mount and whenever deps change. Returns { data, error, loading, reload }. */
export function useApi(loader, deps = []) {
  const [state, setState] = useState({ data: null, error: null, loading: true });
  const run = useCallback(() => {
    setState((s) => ({ ...s, loading: true, error: null }));
    let alive = true;
    loader().then(
      (data) => alive && setState({ data, error: null, loading: false }),
      (error) => alive && setState({ data: null, error, loading: false }),
    );
    return () => { alive = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  useEffect(() => run(), [run]);
  return { ...state, reload: run };
}

export function useAction() {
  const [state, setState] = useState({ result: null, error: null, busy: false });
  const run = useCallback(async (fn) => {
    setState({ result: null, error: null, busy: true });
    try {
      const result = await fn();
      setState({ result, error: null, busy: false });
      return result ?? true;
    } catch (error) {
      setState({ result: null, error, busy: false });
      return false;
    }
  }, []);
  return { ...state, run };
}
