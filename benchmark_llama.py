#!/usr/bin/env python3
"""Performance benchmark script for llama-server with different models."""

import subprocess
import time
import json
import argparse
from pathlib import Path


def run_benchmark(model_name: str, config: dict, iterations: int = 3) -> dict:
    """Run performance benchmark for a specific model configuration."""
    results = {
        "model": model_name,
        "config": config,
        "iterations": []
    }
    
    # Start server
    cmd = ["llama-server", "-hf", model_name]
    for key, value in config.items():
        cmd.extend([f"--{key.replace('_', '-')}", str(value)])
    
    print(f"Starting server with command: {' '.join(cmd)}")
    
    server_process = subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True
    )
    
    # Wait for server to start
    time.sleep(3)
    
    try:
        for i in range(iterations):
            print(f"\n--- Iteration {i + 1}/{iterations} ---")
            
            # Send a test request
            start_time = time.time()
            
            result = subprocess.run(
                [
                    "curl", "-s", "-X", "POST",
                    "http://localhost:8080/v1/chat/completions",
                    "-H", "Content-Type: application/json",
                    "-d", '{"messages":[{"role":"user","content":"Hello"}],"model":"default","max_tokens":50}'
                ],
                capture_output=True,
                text=True,
                timeout=60
            )
            
            elapsed = time.time() - start_time
            
            iteration_result = {
                "iteration": i + 1,
                "time_seconds": elapsed,
                "status": "success" if result.returncode == 0 else "failed",
                "output": result.stdout[:500] if result.stdout else ""
            }
            results["iterations"].append(iteration_result)
            
            print(f"Iteration {i + 1} completed in {elapsed:.2f} seconds")
            
            # Small delay between iterations
            time.sleep(1)
    
    finally:
        # Stop server
        server_process.terminate()
        try:
            server_process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            server_process.kill()
    
    return results


def main():
    parser = argparse.ArgumentParser(description="Benchmark llama-server performance")
    parser.add_argument("--iterations", "-i", type=int, default=3, 
                       help="Number of iterations per benchmark")
    parser.add_argument("--output", "-o", default="benchmark_results.json",
                       help="Output file for results")
    
    args = parser.parse_args()
    
    # Define models and configurations to test
    benchmarks = [
        {
            "name": "Qwen3-Coder-Next-REAM-i1-GGUF:Q3_K_S",
            "config": {
                "n_gpu_layers": 999,
                "n_cpu_moe": 35,
                "no_mmap": "",
                "fa": "on",
                "cache_type_k": "q8_0",
                "cache_type_v": "turbo2",
                "ctx_size": 262144
            }
        },
        {
            "name": "Qwen3-Coder-Next-REAM-i1-GGUF:Q3_K_S",
            "config": {
                "n_gpu_layers": 50,
                "n_cpu_moe": 8,
                "no_mmap": "",
                "fa": "on",
                "cache_type_k": "q8_0",
                "cache_type_v": "turbo2",
                "ctx_size": 262144
            }
        },
        {
            "name": "Qwen3-Coder-Next-REAM-i1-GGUF:Q3_K_S",
            "config": {
                "n_gpu_layers": 0,
                "n_cpu_moe": 16,
                "no_mmap": "",
                "fa": "on",
                "cache_type_k": "q8_0",
                "cache_type_v": "turbo2",
                "ctx_size": 131072
            }
        }
    ]
    
    all_results = []
    
    for benchmark in benchmarks:
        print(f"\n{'='*60}")
        print(f"Running benchmark for {benchmark['name']}")
        print(f"{'='*60}")
        
        result = run_benchmark(
            benchmark["name"],
            benchmark["config"],
            args.iterations
        )
        all_results.append(result)
        
        # Save intermediate results
        with open(args.output, "w") as f:
            json.dump(all_results, f, indent=2)
    
    # Save final results
    with open(args.output, "w") as f:
        json.dump(all_results, f, indent=2)
    
    print(f"\n\nBenchmark complete! Results saved to {args.output}")
    
    # Print summary
    print("\n\n=== BENCHMARK SUMMARY ===")
    for result in all_results:
        times = [it["time_seconds"] for it in result["iterations"]]
        avg_time = sum(times) / len(times)
        print(f"\n{result['model']}")
        print(f"  Config: {result['config']}")
        print(f"  Average time: {avg_time:.2f} seconds")
        print(f"  Min time: {min(times):.2f} seconds")
        print(f"  Max time: {max(times):.2f} seconds")


if __name__ == "__main__":
    main()
