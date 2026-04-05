package com.sebastiandorata.musicdashboard.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic DoublyLinkedList data structure for pagination.
 *
 * Purpose: Enables efficient forward/backward traversal through large result sets,
 * supporting use cases like "view first/last N items" or "go to previous/next page".
 *
 * Time Complexity:
 *   - Access by index: O(n) in worst case (traversal required)
 *   - Insert at head/tail: O(1)
 *   - Insert at position: O(n)
 *   - Delete at position: O(n)
 *   - Next/previous page: O(1) amortized
 *
 * Space Complexity: O(n) where n = number of elements
 *
 * Use case: Analytics listings (songs, artists, albums, history)
 * where users paginate through results and may traverse backward.
 */
public class DoublyLinkedList<T> {

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public DoublyLinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    /**
     * Appends {@code value} to the tail of the list.
     *
     * <p>Time Complexity: O(1)
     * Space Complexity: O(1)
     *
     * @param value the value to append
     */
    public void add(T value) {
        Node<T> newNode = new Node<>(value);
        if (isEmpty()) {
            head = tail = newNode;
        } else {
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        }
        size++;
    }

    /**
     * Prepends {@code value} to the head of the list.
     *
     * <p>Time Complexity: O(1)
     * Space Complexity: O(1)
     *
     * @param value the value to prepend
     */
    public void addFirst(T value) {
        Node<T> newNode = new Node<>(value);
        if (isEmpty()) {
            head = tail = newNode;
        } else {
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        }
        size++;
    }

    /**
     * Returns the element at the given index using bidirectional traversal.
     * Traversal starts from the head if {@code index < size / 2},
     * or from the tail otherwise.
     *
     * <p>Time Complexity: O(n) worst case; O(n/2) average
     *
     * @param index the zero-based index of the element to retrieve
     * @return the element at {@code index}, or {@code null} if out of bounds
     */
    public T get(int index) {
        if (index < 0 || index >= size) return null;

        Node<T> current;
        if (index < size / 2) {
            current = head;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
        } else {
            current = tail;
            for (int i = size - 1; i > index; i--) {
                current = current.prev;
            }
        }
        return current.value;
    }

    /**
     * Returns the first element in the list without removing it.
     *
     * <p>Time Complexity: O(1)
     *
     * @return the head element, or {@code null} if the list is empty
     */
    public T getFirst() {
        return isEmpty() ? null : head.value;
    }

    /**
     * Returns the last element in the list without removing it.
     *
     * <p>Time Complexity: O(1)
     *
     * @return the tail element, or {@code null} if the list is empty
     */
    public T getLast() {
        return isEmpty() ? null : tail.value;
    }

    /**
     * Returns a window (page) of elements from the list.
     * Time Complexity: O(offset + limit)
     * Space Complexity: O(limit)
     */
    public List<T> getWindow(int offset, int limit) {
        List<T> window = new ArrayList<>();

        if (offset < 0 || offset >= size) {
            return window;
        }

        Node<T> current = head;
        for (int i = 0; i < offset; i++) {
            current = current.next;
        }

        for (int i = 0; i < limit && current != null; i++) {
            window.add(current.value);
            current = current.next;
        }

        return window;
    }

    /**
     * Removes and returns the first element in the list.
     *
     * <p>Time Complexity: O(1)
     * Space Complexity: O(1)
     *
     * @return the removed head element, or {@code null} if the list is empty
     */
    public T removeFirst() {
        if (isEmpty()) return null;

        T value = head.value;
        if (head == tail) {
            head = tail = null;
        } else {
            head = head.next;
            head.prev = null;
        }
        size--;
        return value;
    }

    /**
     * Removes and returns the last element in the list.
     *
     * <p>Time Complexity: O(1)
     * Space Complexity: O(1)
     *
     * @return the removed tail element, or {@code null} if the list is empty
     */
    public T removeLast() {
        if (isEmpty()) return null;

        T value = tail.value;
        if (head == tail) {
            head = tail = null;
        } else {
            tail = tail.prev;
            tail.next = null;
        }
        size--;
        return value;
    }


    /**
     * Returns {@code true} if the list contains no elements.
     *
     * <p>Time Complexity: O(1)
     *
     * @return {@code true} if the list is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns {@code true} if the list contains an element equal to
     * {@code value} according to {@link Object#equals(Object)}.
     *
     * <p>Time Complexity: O(n)
     *
     * @param value the value to search for
     * @return {@code true} if a matching element is found
     */
    public boolean contains(T value) {
        Node<T> current = head;
        while (current != null) {
            if (current.value.equals(value)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * Returns all elements as a new {@link java.util.List} in head-to-tail order.
     *
     * <p>Time Complexity: O(n)
     * Space Complexity: O(n)
     *
     * @return a {@link java.util.List} containing all elements in order
     */
    public List<T> toList() {
        List<T> result = new ArrayList<>();
        Node<T> current = head;
        while (current != null) {
            result.add(current.value);
            current = current.next;
        }
        return result;
    }

    private static class Node<T> {
        T value;
        Node<T> next;
        Node<T> prev;

        Node(T value) {
            this.value = value;
            this.next = null;
            this.prev = null;
        }
    }
}