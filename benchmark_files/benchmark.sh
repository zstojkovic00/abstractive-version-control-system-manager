#!/bin/bash

measure_time() {
  start_time=$(date +%s%N)
  "$@"
  end_time=$(date +%s%N)
  echo $((end_time - start_time))
}

benchmark_files_dir="/home/zstojkovic00/IdeaProjects/abstractive-version-control-system-manager/benchmark_files"

echo "Compressing file_small.bin..."
compress_time=$(measure_time git hash-object -w --stdin --path "$benchmark_files_dir/file_small.bin" < "$benchmark_files_dir/file_small.bin")
compressed_hash=$(git hash-object -w --stdin --path "$benchmark_files_dir/file_small.bin" < "$benchmark_files_dir/file_small.bin")
compressed_size=$(git cat-file -s "$compressed_hash")
echo "Execution time for file_small.bin: $compress_time ns"
echo "Compressed size of file_small.bin: $compressed_size bytes"

echo "Compressing file_large.bin..."
compress_time=$(measure_time git hash-object -w --stdin --path "$benchmark_files_dir/file_large.bin" < "$benchmark_files_dir/file_large.bin")
compressed_hash=$(git hash-object -w --stdin --path "$benchmark_files_dir/file_large.bin" < "$benchmark_files_dir/file_large.bin")
compressed_size=$(git cat-file -s "$compressed_hash")
echo "Execution time for file_large.bin: $compress_time ns"
echo "Compressed size of file_large.bin: $compressed_size bytes"

echo "Compressing file_xlarge.bin..."
compress_time=$(measure_time git hash-object -w --stdin --path "$benchmark_files_dir/file_xlarge.bin" < "$benchmark_files_dir/file_xlarge.bin")
compressed_hash=$(git hash-object -w --stdin --path "$benchmark_files_dir/file_xlarge.bin" < "$benchmark_files_dir/file_xlarge.bin")
compressed_size=$(git cat-file -s "$compressed_hash")
echo "Execution time for file_xlarge.bin: $compress_time ns"
echo "Compressed size of file_xlarge.bin: $compressed_size bytes"
