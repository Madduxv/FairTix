import api from './client';

function mockResponse(status, body) {
  const ok = status >= 200 && status < 300;
  return Promise.resolve({
    ok,
    status,
    json: () => Promise.resolve(body),
  });
}

beforeEach(() => {
  global.fetch = jest.fn();
});

afterEach(() => {
  jest.resetAllMocks();
});

test('GET request returns parsed JSON on success', async () => {
  global.fetch.mockReturnValue(mockResponse(200, { id: 1, name: 'test' }));
  const result = await api.get('/api/resource');
  expect(result).toEqual({ id: 1, name: 'test' });
  expect(fetch).toHaveBeenCalledWith(
    '/api/resource',
    expect.objectContaining({ method: 'GET', credentials: 'include' })
  );
});

test('POST request sends JSON body with Content-Type header', async () => {
  global.fetch.mockReturnValue(mockResponse(200, { success: true }));
  await api.post('/api/resource', { key: 'value' });
  expect(fetch).toHaveBeenCalledWith(
    '/api/resource',
    expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ key: 'value' }),
      headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
    })
  );
});

test('204 response returns null without parsing body', async () => {
  global.fetch.mockReturnValue(
    Promise.resolve({ ok: true, status: 204, json: () => Promise.reject(new Error('no body')) })
  );
  const result = await api.delete('/api/resource');
  expect(result).toBeNull();
});

test('throws error with status and message on non-ok response', async () => {
  global.fetch.mockReturnValue(mockResponse(404, { message: 'Not found' }));
  await expect(api.get('/api/missing')).rejects.toMatchObject({
    status: 404,
    message: 'Not found',
  });
});

test('uses failureReason as fallback message when message is absent', async () => {
  global.fetch.mockReturnValue(mockResponse(400, { failureReason: 'Bad input' }));
  await expect(api.post('/api/action', {})).rejects.toMatchObject({
    status: 400,
    message: 'Bad input',
  });
});

test('401 on a non-auth path triggers a refresh attempt then retries the original request', async () => {
  global.fetch
    .mockReturnValueOnce(mockResponse(401, {}))          // original fails with 401
    .mockReturnValueOnce(mockResponse(200, {}))          // refresh succeeds
    .mockReturnValueOnce(mockResponse(200, { ok: true })); // retry returns data

  const result = await api.get('/api/protected');
  expect(result).toEqual({ ok: true });
  expect(fetch).toHaveBeenCalledTimes(3);
});

test('dispatches auth:session-expired when refresh also fails', async () => {
  const listener = jest.fn();
  window.addEventListener('auth:session-expired', listener);

  global.fetch
    .mockReturnValueOnce(mockResponse(401, {}))  // original fails
    .mockReturnValueOnce(mockResponse(401, {})); // refresh fails

  await expect(api.get('/api/protected')).rejects.toBeDefined();
  expect(listener).toHaveBeenCalled();

  window.removeEventListener('auth:session-expired', listener);
});

test('skips refresh and propagates error for /auth/login 401', async () => {
  global.fetch.mockReturnValue(mockResponse(401, { message: 'Bad credentials' }));
  await expect(api.post('/auth/login', {})).rejects.toMatchObject({ status: 401 });
  expect(fetch).toHaveBeenCalledTimes(1);
});
