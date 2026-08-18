package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"os"
	"time"
)

const baseURL = "https://api.checknumber.ai"

type task struct {
	TaskID    string `json:"task_id"`
	Status    string `json:"status"`
	ResultURL string `json:"result_url"`
}

func mustAPIKey() string {
	key := os.Getenv("CHECKNUMBER_API_KEY")
	if key == "" {
		fmt.Fprintln(os.Stderr, "Set the CHECKNUMBER_API_KEY environment variable")
		os.Exit(1)
	}
	return key
}

func doJSON(req *http.Request) task {
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		fmt.Fprintln(os.Stderr, "request error:", err)
		os.Exit(1)
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)
	if resp.StatusCode >= 300 {
		fmt.Fprintf(os.Stderr, "request failed: %d %s\n", resp.StatusCode, body)
		os.Exit(1)
	}
	var t task
	if err := json.Unmarshal(body, &t); err != nil {
		fmt.Fprintln(os.Stderr, "invalid JSON response:", err)
		os.Exit(1)
	}
	return t
}

func submit(key string) task {
	f, err := os.Open("../numbers.txt")
	if err != nil {
		fmt.Fprintln(os.Stderr, "open input file:", err)
		os.Exit(1)
	}
	defer f.Close()

	var buf bytes.Buffer
	w := multipart.NewWriter(&buf)
	part, _ := w.CreateFormFile("file", "numbers.txt")
	io.Copy(part, f)
	w.WriteField("task_type", "vk")
	w.Close()

	req, _ := http.NewRequest("POST", baseURL+"/v1/tasks", &buf)
	req.Header.Set("X-API-Key", key)
	req.Header.Set("Content-Type", w.FormDataContentType())
	return doJSON(req)
}

func poll(key, taskID string) task {
	form := "task_id=" + taskID
	req, _ := http.NewRequest("POST", baseURL+"/v1/gettasks", bytes.NewBufferString(form))
	req.Header.Set("X-API-Key", key)
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	return doJSON(req)
}

func download(url, dest string) {
	resp, err := http.Get(url)
	if err != nil {
		fmt.Fprintln(os.Stderr, "download error:", err)
		os.Exit(1)
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		fmt.Fprintf(os.Stderr, "download failed: %d\n", resp.StatusCode)
		os.Exit(1)
	}
	out, err := os.Create(dest)
	if err != nil {
		fmt.Fprintln(os.Stderr, "create output file:", err)
		os.Exit(1)
	}
	defer out.Close()
	io.Copy(out, resp.Body)
}

func main() {
	key := mustAPIKey()

	t := submit(key)
	fmt.Printf("submitted task %s status=%s\n", t.TaskID, t.Status)

	for t.Status != "exported" && t.Status != "failed" {
		time.Sleep(5 * time.Second)
		t = poll(key, t.TaskID)
		fmt.Println("status=" + t.Status)
	}

	if t.Status == "failed" {
		fmt.Fprintln(os.Stderr, "task failed")
		os.Exit(1)
	}
	if t.ResultURL == "" {
		fmt.Fprintln(os.Stderr, "task exported but no result_url")
		os.Exit(1)
	}

	download(t.ResultURL, "results.zip")
	fmt.Println("saved results.zip")
}
