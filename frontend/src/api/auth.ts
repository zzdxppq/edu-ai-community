import http from '@/api';
import type {
  ApiEnvelope,
  RegisterRequest,
  SendSmsRequest,
  UserDto,
} from '@/types/auth';

export async function sendSms(phone: string): Promise<ApiEnvelope<null>> {
  const payload: SendSmsRequest = { phone };
  const response = await http.post<ApiEnvelope<null>>('/api/auth/send-sms', payload);
  return response.data;
}

export async function register(
  payload: RegisterRequest,
): Promise<ApiEnvelope<UserDto>> {
  const response = await http.post<ApiEnvelope<UserDto>>(
    '/api/auth/register',
    payload,
  );
  return response.data;
}
