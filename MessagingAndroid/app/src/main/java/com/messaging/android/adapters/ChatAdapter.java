package com.messaging.android.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.messaging.android.R;
import com.messaging.android.services.FileTransferService;
import com.messaging.models.Message;
import com.messaging.models.MessageType;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_TEXT_SENT = 1;
    private static final int VIEW_TYPE_TEXT_RECEIVED = 2;
    private static final int VIEW_TYPE_IMAGE_SENT = 3;
    private static final int VIEW_TYPE_IMAGE_RECEIVED = 4;
    private static final int VIEW_TYPE_FILE_SENT = 5;
    private static final int VIEW_TYPE_FILE_RECEIVED = 6;

    private final List<Message> messages;
    private final String currentUserId;
    private final Context context;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public ChatAdapter(List<Message> messages, String currentUserId, Context context) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        boolean isSent = message.getSenderId().equals(currentUserId);

        switch (message.getType()) {
            case IMAGE:
                return isSent ? VIEW_TYPE_IMAGE_SENT : VIEW_TYPE_IMAGE_RECEIVED;
            case FILE:
            case AUDIO:
                return isSent ? VIEW_TYPE_FILE_SENT : VIEW_TYPE_FILE_RECEIVED;
            default:
                return isSent ? VIEW_TYPE_TEXT_SENT : VIEW_TYPE_TEXT_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_TEXT_SENT:
                return new TextSentViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false));
            case VIEW_TYPE_TEXT_RECEIVED:
                return new TextReceivedViewHolder(inflater.inflate(R.layout.item_message_received, parent, false));
            case VIEW_TYPE_IMAGE_SENT:
                return new ImageSentViewHolder(inflater.inflate(R.layout.item_image_sent, parent, false));
            case VIEW_TYPE_IMAGE_RECEIVED:
                return new ImageReceivedViewHolder(inflater.inflate(R.layout.item_image_received, parent, false));
            case VIEW_TYPE_FILE_SENT:
                return new FileSentViewHolder(inflater.inflate(R.layout.item_file_sent, parent, false));
            case VIEW_TYPE_FILE_RECEIVED:
                return new FileReceivedViewHolder(inflater.inflate(R.layout.item_file_received, parent, false));
            default:
                return new TextSentViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder instanceof TextSentViewHolder) {
            ((TextSentViewHolder) holder).bind(message);
        } else if (holder instanceof TextReceivedViewHolder) {
            ((TextReceivedViewHolder) holder).bind(message);
        } else if (holder instanceof ImageSentViewHolder) {
            ((ImageSentViewHolder) holder).bind(message);
        } else if (holder instanceof ImageReceivedViewHolder) {
            ((ImageReceivedViewHolder) holder).bind(message);
        } else if (holder instanceof FileSentViewHolder) {
            ((FileSentViewHolder) holder).bind(message);
        } else if (holder instanceof FileReceivedViewHolder) {
            ((FileReceivedViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ========== ViewHolder Classes ==========

    class TextSentViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        public TextSentViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
        }

        public void bind(Message message) {
            messageText.setText(message.getContent());
            timeText.setText(message.getTimestamp().format(timeFormatter));
        }
    }

    class TextReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, senderText, timeText;

        public TextReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            senderText = itemView.findViewById(R.id.senderText);
            timeText = itemView.findViewById(R.id.timeText);
        }

        public void bind(Message message) {
            messageText.setText(message.getContent());
            senderText.setText(message.getSenderId());
            timeText.setText(message.getTimestamp().format(timeFormatter));
        }
    }

    class ImageSentViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView fileNameText, timeText;

        public ImageSentViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            timeText = itemView.findViewById(R.id.timeText);
        }

        public void bind(Message message) {
            fileNameText.setText("📷 " + message.getFileName());
            timeText.setText(message.getTimestamp().format(timeFormatter));

            // Load image (sent images aren't saved locally, so we can't display them)
            // You could save a copy or keep the byte array in memory if needed
            imageView.setVisibility(View.GONE);
        }
    }

    class ImageReceivedViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView senderText, fileNameText, timeText;
        Button downloadButton;

        public ImageReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            senderText = itemView.findViewById(R.id.senderText);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            timeText = itemView.findViewById(R.id.timeText);
            downloadButton = itemView.findViewById(R.id.downloadButton);
        }

        public void bind(Message message) {
            senderText.setText(message.getSenderId());
            fileNameText.setText("📷 " + message.getFileName());
            timeText.setText(message.getTimestamp().format(timeFormatter));

            // Load image from saved file
            FileTransferService fileService = new FileTransferService(context);
            File imageFile = fileService.getFile(message.getFileName());

            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);

                // Open image on click
                imageView.setOnClickListener(v -> openImage(imageFile));

                downloadButton.setOnClickListener(v -> openImage(imageFile));
            } else {
                imageView.setVisibility(View.GONE);
                downloadButton.setText("Image not found");
                downloadButton.setEnabled(false);
            }
        }

        private void openImage(File imageFile) {
            try {
                Uri imageUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        imageFile
                );

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(imageUri, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(context, "Failed to open image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    class FileSentViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameText, fileSizeText, timeText;

        public FileSentViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            fileSizeText = itemView.findViewById(R.id.fileSizeText);
            timeText = itemView.findViewById(R.id.timeText);
        }

        public void bind(Message message) {
            String icon = message.getType() == MessageType.AUDIO ? "🎵" : "📎";
            fileNameText.setText(icon + " " + message.getFileName());
            fileSizeText.setText(FileTransferService.formatFileSize(message.getFileSize()));
            timeText.setText(message.getTimestamp().format(timeFormatter));
        }
    }

    class FileReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView senderText, fileNameText, fileSizeText, timeText;
        Button downloadButton;

        public FileReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            senderText = itemView.findViewById(R.id.senderText);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            fileSizeText = itemView.findViewById(R.id.fileSizeText);
            timeText = itemView.findViewById(R.id.timeText);
            downloadButton = itemView.findViewById(R.id.downloadButton);
        }

        public void bind(Message message) {
            String icon = message.getType() == MessageType.AUDIO ? "🎵" : "📎";
            senderText.setText(message.getSenderId());
            fileNameText.setText(icon + " " + message.getFileName());
            fileSizeText.setText(FileTransferService.formatFileSize(message.getFileSize()));
            timeText.setText(message.getTimestamp().format(timeFormatter));

            FileTransferService fileService = new FileTransferService(context);
            File file = fileService.getFile(message.getFileName());

            if (file.exists()) {
                downloadButton.setText("Open File");
                downloadButton.setOnClickListener(v -> openFile(file));
            } else {
                downloadButton.setText("File not found");
                downloadButton.setEnabled(false);
            }
        }

        private void openFile(File file) {
            try {
                Uri fileUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        file
                );

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, getMimeType(file.getName()));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(intent, "Open file with"));

            } catch (Exception e) {
                Toast.makeText(context, "Failed to open file", Toast.LENGTH_SHORT).show();
            }
        }

        private String getMimeType(String fileName) {
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            switch (extension) {
                case "pdf": return "application/pdf";
                case "doc":
                case "docx": return "application/msword";
                case "txt": return "text/plain";
                case "mp3": return "audio/mpeg";
                case "mp4": return "video/mp4";
                case "jpg":
                case "jpeg": return "image/jpeg";
                case "png": return "image/png";
                default: return "*/*";
            }
        }
    }
}
