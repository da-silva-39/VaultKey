package mz.ac.ucm.feng.ti.vaultkey.presentation.authenticator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.authenticator.uri.OtpAuthParseException;
import mz.ac.ucm.feng.ti.vaultkey.authenticator.uri.OtpAuthUri;

/**
 * Leitura de QR Code otpauth:// com a API Camera do framework + ZXing
 * (100% offline, sem dependências novas). A permissão da câmara é pedida
 * apenas aqui; se recusada, indica o cadastro manual. O conteúdo é
 * validado antes de qualquer uso.
 */
public class QrScanFragment extends Fragment implements SurfaceHolder.Callback,
        Camera.PreviewCallback {

    public static final String RESULT_KEY = "qr_result";
    private static final long THROTTLE_MS = 350;

    private SurfaceView surfaceView;
    private Camera camera;
    private boolean handled;
    private long lastScan;
    private final QRCodeReader reader = new QRCodeReader();
    private ActivityResultLauncher<String> permissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_scan, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        openCamera();
                    } else if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.auth_camera_denied,
                                Toast.LENGTH_LONG).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        surfaceView = view.findViewById(R.id.surface);
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (surfaceView != null && hasPermission()) {
            openCamera();
        } else if (!hasPermission()) {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ---- SurfaceHolder.Callback ----

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (hasPermission()) {
            openCamera();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        releaseCamera();
    }

    @SuppressWarnings("deprecation")
    private synchronized void openCamera() {
        if (camera != null || !isAdded()) {
            return;
        }
        try {
            int cameraId = backCameraId();
            camera = Camera.open(cameraId);
            Camera.Parameters params = camera.getParameters();
            Camera.Size preview = choosePreviewSize(params);
            if (preview != null) {
                params.setPreviewSize(preview.width, preview.height);
            }
            try {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } catch (Exception ignored) {
            }
            camera.setParameters(params);
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(surfaceView.getHolder());
            int frameBytes = camera.getParameters().getPreviewSize().width
                    * camera.getParameters().getPreviewSize().height * 3 / 2;
            for (int i = 0; i < 3; i++) {
                camera.addCallbackBuffer(new byte[frameBytes]);
            }
            camera.setPreviewCallbackWithBuffer(this);
            camera.startPreview();
        } catch (Exception e) {
            releaseCamera();
            if (isAdded()) {
                Toast.makeText(requireContext(), R.string.error_generic,
                        Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static int backCameraId() {
        int count = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < count; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return 0;
    }

    @SuppressWarnings("deprecation")
    private static Camera.Size choosePreviewSize(Camera.Parameters params) {
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        Camera.Size best = sizes.get(0);
        for (Camera.Size s : sizes) {
            if (s.width >= 640 && s.width <= 1280 && s.width * s.height
                    > best.width * best.height) {
                best = s;
            }
        }
        return best;
    }

    // ---- Camera.PreviewCallback (NV21) ----

    @Override
    @SuppressWarnings("deprecation")
    public void onPreviewFrame(byte[] data, Camera cam) {
        if (cam != null) {
            cam.addCallbackBuffer(data);
        }
        if (handled || data == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastScan < THROTTLE_MS) {
            return;
        }
        lastScan = now;
        Camera.Size size;
        try {
            size = cam.getParameters().getPreviewSize();
        } catch (Exception e) {
            return;
        }
        String text = decodeQr(data, size.width, size.height);
        if (text != null) {
            onQrText(text);
        }
    }

    private String decodeQr(byte[] nv21, int width, int height) {
        try {
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    nv21, width, height, 0, 0, width, height, false);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            return reader.decode(bitmap).getText();
        } catch (ReaderException e) {
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            reader.reset();
        }
    }

    private void onQrText(String text) {
        OtpAuthUri parsed;
        try {
            parsed = OtpAuthUri.parse(text);
        } catch (OtpAuthParseException e) {
            handled = true;
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.auth_qr_invalid,
                            Toast.LENGTH_LONG).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
            }
            return;
        }
        handled = true;
        Bundle result = new Bundle();
        result.putString("issuer", parsed.getIssuer());
        result.putString("account", parsed.getAccount());
        result.putString("secret", parsed.getSecret());
        result.putString("type", parsed.getType() == OtpAuthUri.Type.HOTP ? "HOTP" : "TOTP");
        result.putString("algorithm", parsed.getAlgorithm());
        result.putInt("digits", parsed.getDigits());
        result.putInt("period", parsed.getPeriod());
        result.putLong("counter", parsed.getCounter());
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    @SuppressWarnings("deprecation")
    private synchronized void releaseCamera() {
        if (camera != null) {
            try {
                camera.setPreviewCallbackWithBuffer(null);
                camera.stopPreview();
                camera.release();
            } catch (Exception ignored) {
            }
            camera = null;
        }
    }
}
