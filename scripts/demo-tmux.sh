#!/usr/bin/env bash
# Lays out a tmux session for the Data Contracts conference demo:
#   window "build"        - two panes: bootstrap-v1 | bootstrap-v2
#   window "v1-demo"       - consumer-v1 | producer-v1, side by side
#   window "v2-evolution"  - consumer-v1 | consumer-v2, with producer-v1 below
#
# Every pane's command is staged (typed) but NOT executed - press Enter in a
# pane when you're ready to narrate it, instead of everything firing at once.
set -euo pipefail

SESSION="data-contracts-demo"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

stage() { # <pane> <title> <command>
  tmux select-pane -t "$1" -T "$2"
  tmux send-keys -t "$1" "$3"
}

if tmux has-session -t "$SESSION" 2>/dev/null; then
  echo "Session '$SESSION' already exists - attaching."
  echo "(Run 'tmux kill-session -t $SESSION' first if you want a clean layout.)"
  exec tmux attach -t "$SESSION"
fi

tmux new-session -d -s "$SESSION" -n build -c "$ROOT"
tmux set-option -t "$SESSION" pane-border-status top
tmux set-option -t "$SESSION" pane-border-format "#{pane_title}"

tmux split-window -h -t "$SESSION:build" -c "$ROOT"
stage "$SESSION:build.0" "build v1" "make bootstrap-v1"
stage "$SESSION:build.1" "build v2" "make bootstrap-v2"

tmux new-window -t "$SESSION" -n v1-demo -c "$ROOT"
tmux split-window -h -t "$SESSION:v1-demo" -c "$ROOT"
stage "$SESSION:v1-demo.0" "consumer v1" "make demo-consumer-v1"
stage "$SESSION:v1-demo.1" "producer v1" "make demo-producer-v1"

tmux new-window -t "$SESSION" -n v2-evolution -c "$ROOT"
tmux split-window -h -t "$SESSION:v2-evolution" -c "$ROOT"
tmux split-window -v -t "$SESSION:v2-evolution.1" -c "$ROOT"
tmux select-layout -t "$SESSION:v2-evolution" main-vertical
stage "$SESSION:v2-evolution.0" "consumer v1 (before)" "make demo-consumer-v1"
stage "$SESSION:v2-evolution.1" "consumer v2 (migrated)" "make demo-consumer-v2"
stage "$SESSION:v2-evolution.2" "producer v1" "make demo-producer-v1"

tmux select-window -t "$SESSION:build"
tmux attach -t "$SESSION"
