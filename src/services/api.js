const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://192.168.10.137:8083/api';

// Helper function for handling API responses
const handleResponse = async (response) => {
    console.log('Response status:', response.status);
    console.log('Response headers:', Object.fromEntries(response.headers.entries()));
    
    if (response.status === 403) {
        window.location.href = '/login';
        return;
    }
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        console.error('Error response:', errorData);
        throw new Error(errorData.message || 'Something went wrong');
    }
    const data = await response.json();
    console.log('Response data:', data);
    return data;
};

// Helper function for adding auth headers
const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    return {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };
};

// API methods
export const api = {
    // Auth methods
    login: async (credentials) => {
        console.log('Attempting login with:', credentials);
        console.log('API URL:', `${API_BASE_URL}/auth/login`);
        
        try {
            const response = await fetch(`${API_BASE_URL}/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                },
                body: JSON.stringify(credentials)
            });
            return handleResponse(response);
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    },

    // ... rest of the code ...
}; 