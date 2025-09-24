package com.codegym.socialmedia.model.conversation;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer attachmentId;

    private String attachmentUrl;
    private String fileName;

    private Long fileSize;  // Added to entity to persist file size

    @ManyToOne
    @JoinColumn(name = "message_id")
    @JsonBackReference
    private Message message;

    @Enumerated(EnumType.STRING)
    private Message.MessageType messageType;  // Reference Message.MessageType
}

