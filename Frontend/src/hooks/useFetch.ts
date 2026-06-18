"use client";

import { useCallback, useState } from "react";

export function useFetch<TData>(initialData: TData | null = null) {
  const [data, setData] = useState<TData | null>(initialData);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const execute = useCallback(async (request: () => Promise<TData>) => {
    setLoading(true);
    setError(null);

    try {
      const result = await request();
      setData(result);
      return result;
    } catch (requestError) {
      const message = requestError instanceof Error ? requestError.message : "Đã xảy ra lỗi khi gọi API";
      setError(message);
      throw requestError;
    } finally {
      setLoading(false);
    }
  }, []);

  const reset = useCallback(() => {
    setData(initialData);
    setError(null);
    setLoading(false);
  }, [initialData]);

  return {
    data,
    loading,
    error,
    setData,
    setError,
    execute,
    reset,
  };
}
