import { API_BASE_URL } from '../config';

const API_URL = `${API_BASE_URL}/organizations`;

export async function getAllOrganizations() {
  const response = await fetch(API_URL);
  if (!response.ok) throw new Error('Ошибка при получении организаций');
  return await response.json();
}

export async function createOrganization(orgData) {
  const response = await fetch(API_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(orgData),
  });
  if (!response.ok) throw new Error('Ошибка при создании организации');
  return await response.json();
} 