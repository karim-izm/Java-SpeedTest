# Java Network Speed Test CLI

A lightweight Java command-line tool to test your internet download speed using real-world files from Hetzner's speed test server.

## How It Works

This tool downloads one of several real files (100MB, 1GB, or 10GB) and measures:
- Total download time
- Average download speed
- Peak download speed
- Time to 50% and 100% completion

The file is deleted after the test completes to avoid using disk space.

## Usage

### Requirements

- Java Development Kit (JDK) 17 or later
- A terminal or command prompt with `java` in the PATH

### Run the Tool

Download the `speedtest.jar` file from the releases page.

Then run one of the following:

java -jar speedtest.jar --light

css
Copy
Edit

Downloads a 100MB file for a quick test.

java -jar speedtest.jar --standard

css
Copy
Edit

Downloads a 1GB file to test average speeds more reliably.

java -jar speedtest.jar --heavy

bash
Copy
Edit

Downloads a 10GB file to test long-term sustained bandwidth.

### Example Output

Starting network speed test...
Testing: 100% complete, Current speed: 16.10 MB/s
