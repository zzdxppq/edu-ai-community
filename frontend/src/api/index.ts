import axios, { AxiosError, type AxiosInstance } from 'axios';
import { ElMessage } from 'element-plus';

/**
 * User-facing error copy. Kept Chinese-only and deliberately vague so no
 * technical detail (status codes, upstream messages, stack info) leaks into
 * the UI — see Story 1.1 AC2 Error Handling contract.
 */
export const NETWORK_ERROR_MESSAGE = '服务连接中，请稍后重试';

export const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
});

/**
 * Response interceptor error branch, exported so unit tests can invoke it
 * directly without spinning up a full Axios request cycle.
 */
export function handleResponseError(error: AxiosError): Promise<never> {
  if (isNetworkLikeError(error)) {
    ElMessage({ type: 'error', message: NETWORK_ERROR_MESSAGE });
  }
  return Promise.reject(error);
}

http.interceptors.response.use((response) => response, handleResponseError);

function isNetworkLikeError(error: AxiosError): boolean {
  if (!error.response) return true;
  return error.code === 'ECONNABORTED';
}

export default http;
