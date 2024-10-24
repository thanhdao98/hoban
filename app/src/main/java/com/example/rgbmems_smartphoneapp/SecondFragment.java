package com.example.rgbmems_smartphoneapp;

import static android.app.Activity.RESULT_OK;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;



public class SecondFragment extends Fragment {

    private CustomViewPager viewPager;
    // UI Components

    public static final int Neutral = 0;
    public static final int Eraser = 1;
    public static final int BlackPen = 2;

    private int ToolMode = Neutral;   // 描画ツール非選択状態

    private ConnectToServer connectToServer;
    private Spinner numberSpinner; // Declare numberSpinner here
    public static int currentNumber;
    private static final int PICK_IMAGE = 1;
    private Button btnComplete; // Button to complete the action
    private LinearLayout topMenu, bottomMenu; // Layouts for top and bottom menus
    private ImageView imgPen, imgErase; // Icons for pen and eraser  color tools
    private ImageButton imgSelectColor; //

    private SeekBar seekBarThickness; //
    private ImageButton  thickness; //
    private ImageButton btnUndo, btnRedo; // Buttons for undo and redo actions

    // Custom drawing view
    private DrawingView mDrawingView;
    private boolean isMenuVisible = true; // Flag to check if menus are visible
    private int currentColor = Color.BLACK;

    // Image picker launcher
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the default state
        ToolMode = Neutral;
        connectToServer = new ConnectToServer();

        // Connect to server
        try {
            this.connectToServer.connect();
            //connectToServer.connectToServer(getActivity());
        } catch (Exception e) { // Change IOException to Exception if needed
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error connecting to server", Toast.LENGTH_SHORT).show();
        }
        initializeImagePicker();
        showColorPicker();
    }


    // Phương thức riêng để hiển thị SeekBar và xử lý sự kiện
    private void showThicknessAdjustment() {
        seekBarThickness.setVisibility(View.VISIBLE); // Hiển thị SeekBar

        seekBarThickness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Cập nhật độ dày nét vẽ trong DrawingView
                mDrawingView.setBrushThickness(progress); // Cần có phương thức setBrushThickness trong DrawingView
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Có thể thêm logic khi bắt đầu kéo SeekBar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Có thể thêm logic khi ngừng kéo SeekBar
            }
        });
    }

    // Phương thức để ẩn SeekBar
    private void hideThicknessAdjustment() {
        seekBarThickness.setVisibility(View.GONE); // Ẩn SeekBar
    }


    private void showColorPicker() {
        final int[] colors = {
                Color.RED, Color.GREEN, Color.BLUE,
                Color.YELLOW, Color.CYAN, Color.MAGENTA,
                Color.BLACK, Color.WHITE
        };

        // List of color names
        final String[] colorNames = {"赤", "緑", "青", "黄色", "シアン", "マゼンタ", "黒", "白"};

        // Find the index of the current color in the list
        int selectedColorIndex = -1;
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == currentColor) {
                selectedColorIndex = i;
                break;
            }
        }

        // Use requireActivity() to get the context of the activity
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("色を選択"); // "Select Color"

        // Show a dialog with single choice items and highlight the selected color
        builder.setSingleChoiceItems(colorNames, selectedColorIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentColor = colors[which]; // Update the selected color
                mDrawingView.setPaintColor(currentColor); // Update the color of DrawingView
                dialog.dismiss(); // Close the dialog after selection
            }
        });

        builder.show(); // Show the dialog
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_second, container, false);

        // Restore tool states if the fragment was recreated
        if (savedInstanceState != null) {
            ToolMode = Neutral;
        }

        viewPager = getActivity().findViewById(R.id.viewPager);
        initializeUIComponents(rootView);
        setupEventListeners(rootView);
        mDrawingView.setToolMode(ToolMode); // Disable drawing mode (cannot draw)

        // Ensure UI and swipe state are updated based on tool selection
        updateToolSelectionUI();

        return rootView;


    }

    private void initializeImagePicker() {
        // Initialize the image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            mDrawingView.loadImage(imageUri);
                        }
                    }
                }
        );
    }

    /**
     * Initializes UI components for the activity
     */
    private void initializeUIComponents(View rootView) {
        mDrawingView = rootView.findViewById(R.id.drawingView);
        imgPen = rootView.findViewById(R.id.imageButtonPencil);
        seekBarThickness = rootView.findViewById(R.id.seekBarThickness);
        thickness = rootView.findViewById(R.id.imageButtonPenThickness);
        imgErase = rootView.findViewById(R.id.imageButtonEraser);
        imgSelectColor=rootView.findViewById(R.id.imageButtonSelectColor);
        btnUndo = rootView.findViewById(R.id.imageButtonUndo);
        btnRedo = rootView.findViewById(R.id.imageButtonRedo);
        btnComplete = rootView.findViewById(R.id.buttonComplete);
        topMenu = rootView.findViewById(R.id.topMenu);
        bottomMenu = rootView.findViewById(R.id.bottomMenu);

        numberSpinner = rootView.findViewById(R.id.numberSpinner);
        setupNumberSpinner();

        Button selectImageButton = rootView.findViewById(R.id.buttonSelectImage);
        selectImageButton.setOnClickListener(v -> openImageChooser());
        imgSelectColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker();
            }
        });

        mDrawingView.setToolMode(ToolMode); // Disable drawing mode at the start

        // Trong phần khởi tạo nút điều chỉnh độ dày bút
        thickness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showThicknessAdjustment(); // Hiển thị SeekBar để điều chỉnh độ dày bút
            }
        });

    }

    private void setupNumberSpinner() {
        // Create an array of numbers from 90 to 99
        String[] numbers = new String[]{"90", "91", "92", "93", "94", "95", "96", "97", "98", "99"};
        // Use CustomSpinnerAdapter instead of ArrayAdapter
        CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(getActivity(), numbers);
        // Apply the adapter to the spinner
        numberSpinner.setAdapter(adapter);

        // Set an item selected listener on the spinner
//        numberSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                String selectedNumber = (String) parent.getItemAtPosition(position);
//                // Do nothing here, the Spinner will display the selected number
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Do nothing if no item is selected
//            }
//        });
    }

    // Open image chooser
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);

        //背景画像選択時にニュートラル状態に戻す
        ToolMode = Neutral;
        mDrawingView.setToolMode(ToolMode);
        updateToolSelectionUI();
    }

    /**
     * Sets up event listeners for UI components
     */
    private float startx;
    private float starty;
    @SuppressLint("ClickableViewAccessibility")
    private void setupEventListeners(View rootView) {
        if (viewPager == null) {
            Log.d("swipe", "viewPager is null");
        } else {
            Log.d("swipe", "viewPager is initialized");
        }
        // Event listeners for pen and eraser tools
        imgPen.setOnClickListener(v -> selectPen());
        imgErase.setOnClickListener(v -> selectEraser());

        // Event listeners for undo and redo actions
        btnUndo.setOnClickListener(v -> mDrawingView.undo());
        btnRedo.setOnClickListener(v -> mDrawingView.redo());

        // Event listener to toggle the visibility of the menus
        mDrawingView.setOnTouchListener((v, event) -> {
            int TAP_THRESHOLD = 150;   // スワイプで反応しないようにTapか判定する指の移動閾値
            if ( ToolMode != Neutral) {       // ニュートラル状態以外ではTapか判定しない
                return false;
            } else {
                switch ( event.getAction() ) {
                    case MotionEvent.ACTION_DOWN:
                        startx = event.getX();
                        starty = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();
                        float distanceX = Math.abs(endX - startx);
                        float distanceY = Math.abs(endY - starty);
                        if( distanceX < TAP_THRESHOLD && distanceY < TAP_THRESHOLD) {
                            toggleMenus();
                        }
                        break;
                    default:
                        return false;
                }
            }
            return true;
        });

        btnComplete.setOnClickListener(v -> showConfirmationDialog());
    }

    // Event listener for the complete button
    private void showConfirmationDialog() {
        // Show ConfirmationDialog when the user presses the 完了 button
        ConfirmationDialog.show(getActivity(), (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                // Perform sending the image to the server
                Bitmap bitmap = mDrawingView.getBitmap();
                currentNumber = Integer.parseInt((String) numberSpinner.getSelectedItem());
                mDrawingView.saveImage(getActivity());
                //saveImageToGallery(bitmap);
                sendDrawingToServer(); // Call the method to send the image
                // Reset the drawing view and increment the image number
                resetDrawingViewAndIncreaseNumber();
            }
        });
    }

    private void sendDrawingToServer() {
        // Get bitmap from DrawingView
        Bitmap bitmap = mDrawingView.getBitmapFromDrawingView(); // Ensure this method returns Bitmap from mDrawingView

        if (bitmap != null) {
            // Compress bitmap to byte array
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Set compression quality (0-100), 100 means no compression
            int quality = 80; // You can adjust quality as needed
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream); // Compress the bitmap
            byte[] imageData = byteArrayOutputStream.toByteArray(); // Convert Bitmap to byte array

            // Send the image to the server
            connectToServer.sendImage(imageData);
        } else {
            Log.e(TAG, "Bitmap is null, cannot send to server"); // Log error if bitmap is null
        }
    }

    private void resetDrawingViewAndIncreaseNumber() {
        // Reset the DrawingView (clear the current content)
        mDrawingView.clear();

        // Increase the Spinner value
        currentNumber = Integer.parseInt((String) numberSpinner.getSelectedItem());

        // Check if the current value has reached the maximum
        if (currentNumber >= 99) {
            // If the maximum is reached, reset to the minimum value
            currentNumber = 90; // Reset the value to 90
        } else {
            // If not, increase the value by 1
            currentNumber++;
        }

        // Update the new value in the Spinner
        int position = currentNumber - 90; // Get the corresponding position in the Spinner (assuming the Spinner starts from 90)
        numberSpinner.setSelection(position); // Update the Spinner value
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Close the socket connection once the activity is destroyed
        connectToServer.disconnect();
    }

    /**
     * Selects the pen tool and applies the relevant settings
     */
    private void selectPen() {
        if (ToolMode == BlackPen) {
            ToolMode = Neutral;
            seekBarThickness.setVisibility(View.GONE);
        } else {
            ToolMode = BlackPen;
        }

        updateToolSelectionUI();

        if (ToolMode == BlackPen) {
            Log.d("Swipe", "Selecting pen");
            mDrawingView.setToolMode(ToolMode); // Activate drawing mode
            seekBarThickness.setVisibility(View.VISIBLE);

            if (viewPager != null) {
                Log.d("Swipe", "SwipeDisabled");
                viewPager.setSwipeEnabled(false);

                Log.d("SeekBar", "Set visibility to VISIBLE");


                // Start vibration effect on the pen icon
                startAnimation(imgPen);
            }
        } else {
            mDrawingView.setToolMode(ToolMode); // Deactivate drawing mode
            if (viewPager != null) {
                Log.d("Swipe", "SwipeEnabled");
                viewPager.setSwipeEnabled(true);
                seekBarThickness.setVisibility(View.GONE);
            }
        }

        updateToolSelectionUI(); // Update UI and swipe status
        // Lắng nghe sự kiện thay đổi của SeekBar
        seekBarThickness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Cập nhật độ dày nét vẽ trong DrawingView
                    mDrawingView.setBrushThickness(progress); // Cần có phương thức setBrushThickness trong DrawingView
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Có thể thêm logic khi bắt đầu kéo SeekBar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Có thể thêm logic khi ngừng kéo SeekBar
            }
        });
    }



    /**
     * Selects the eraser tool and applies the relevant settings
     */
    private void selectEraser() {
        if ( ToolMode == Eraser ) {
            ToolMode = Neutral;

        } else {
            ToolMode = Eraser;

        }
        updateToolSelectionUI();
        if (ToolMode == Eraser) {
            Log.d("Swipe", "Selecting eraser");
            mDrawingView.setToolMode(ToolMode); // Activate the eraser mode
            // Disable the screen swipe feature when erasing
            if (viewPager != null) {
                Log.d("Swipe", "SwipeDisabled");
                viewPager.setSwipeEnabled(false);

                // Start shake animation on the eraser icon
                startAnimation(imgErase);
            }
        } else {
            mDrawingView.setToolMode(ToolMode); // Disable eraser mode (cannot erase)
            if (viewPager != null) {
                Log.d("Swipe", "SwipeEnabled");
                viewPager.setSwipeEnabled(true);
            }
        }
        updateToolSelectionUI(); // Update the interface and swipe state
    }

    /**
     * Starts the shake animation on the given view
     */
    private void startAnimation(View view) {
        Animation shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
        view.startAnimation(shake);
    }

    private void updateToolSelectionUI() {
        imgPen.setBackgroundColor(ToolMode == BlackPen ? getResources().getColor(R.color.gray) : Color.TRANSPARENT);
        imgErase.setBackgroundColor(ToolMode == Eraser ? getResources().getColor(R.color.gray) : Color.TRANSPARENT);
    }



    /**
     * Toggles the visibility of the top and bottom menus
     */
    public void toggleMenus() {
        if (isMenuVisible) {
            topMenu.setVisibility(View.GONE); // Hide menus
            bottomMenu.setVisibility(View.GONE);
        } else {
            topMenu.setVisibility(View.VISIBLE); // Show menus
            bottomMenu.setVisibility(View.VISIBLE);
        }
        isMenuVisible = !isMenuVisible; // Update visibility flag
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("ToolMode", ToolMode);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Ensure the UI is updated based on the tool selection state
        updateToolSelectionUI();

        // Ensure event listeners are properly set up when returning to the fragment
        View rootView = getView();
        if (viewPager != null) {
            if (ToolMode == Neutral) {
                viewPager.setSwipeEnabled(true);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (viewPager != null) {
            viewPager.setSwipeEnabled(true); // Enable swipe when leaving the fragment
        }
    }

    public int isToolMode() {
        return ToolMode;
    }
}

