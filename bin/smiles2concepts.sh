#!/bin/bash

# Initialize variables
input_file=""
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "$SCRIPT_DIR/.." || exit

# Parse command line options
while getopts "i:" opt; do
    case $opt i
        i) input_file="$OPTARG";;
        *) echo "Run standalone smiles classification. Usage: $0 -i smiles" >&2
           exit 1;;
    esac
done

# Check if input file was provided
if [ -z "$input_file" ]; then
    echo "Error: input smiles not provided" >&2
    echo "Usage: $0 -i smiles" >&2
    exit 1
fi

# Check if the file exists
if [ ! -f "$input_file" ]; then
    echo "File '$input_file' does not exist" >&2
    exit 1
fi

# Get and output only the file size in bytes
java -jar "$SCRIPT_DIR/Molecule2concepts.jar" -i inputSmiles
