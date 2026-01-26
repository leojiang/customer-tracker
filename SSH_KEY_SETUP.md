# SSH Key Setup for Automated Deployment

Setting up SSH keys allows you to deploy without entering passwords every time.

## What Are SSH Keys?

SSH keys are like a password that lives on your computer. Instead of typing your password, your computer proves its identity with a cryptographic key.

**Benefits:**
- ✅ No password prompts during deployment
- ✅ More secure than passwords
- ✅ Faster automated deployments
- ✅ Scripts work without interruption

## Setup: 5 Easy Steps

### Step 1: Generate SSH Key Pair

On your **local Mac**, run:

```bash
ssh-keygen -t ed25519 -a 100 -C "jiangyanghaics@163.com" -f ~/.ssh/id_ed25519_ecs
```

**Press Enter for each prompt:**
- Enter passphrase (optional, press Enter for no passphrase)
- Enter same passphrase again

**What this does:**
- Creates a private key: `~/.ssh/id_ed25519_ecs` (keep this secret!)
- Creates a public key: `~/.ssh/id_ed25519_ecs.pub` (this goes on the server)

### Step 2: Copy Public Key to Server

Run this command:

```bash
ssh-copy-id -i ~/.ssh/id_ed25519_ecs.pub root@47.109.72.216
```

**Enter your password once** when prompted.

**If ssh-copy-id is not available (on some Macs):**

```bash
cat ~/.ssh/id_ed25519_ecs.pub | ssh root@47.109.72.216 'mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys'
```

### Step 3: Test SSH Key Login

```bash
ssh -i ~/.ssh/id_ed25519_ecs root@47.109.72.216
```

**Should connect without password!** 🎉

If it works, type `exit` to disconnect.

### Step 4: Create SSH Config (Optional but Recommended)

This makes SSH commands shorter:

```bash
nano ~/.ssh/config
```

Add these lines:

```
Host ecs
    HostName 47.109.72.216
    User root
    IdentityFile ~/.ssh/id_ed25519_ecs
    ServerAliveInterval 60
    ServerAliveCountMax 3
```

Save with `Ctrl+O`, `Enter`, then `Ctrl+X`.

Now you can connect with just:
```bash
ssh ecs
```

### Step 5: Update Deployment Scripts

Edit the deployment scripts to use your SSH key.

**Option A: Use SSH config (easiest)**

Just change the host in the script:

```bash
# In deploy-frontend-all-in-one.sh
# Change: ECS_HOST="47.109.72.216"
# To: ECS_HOST="ecs"
```

**Option B: Specify key file**

Add `-i` flag to ssh/scp commands:

```bash
ssh -i ~/.ssh/id_ed25519_ecs root@47.109.72.216
scp -i ~/.ssh/id_ed25519_ecs file.txt root@47.109.72.216:/tmp/
```

## Automated Deployment After SSH Setup

Once SSH keys are set up, you can run:

```bash
cd /Users/admin/WorkSpace/ai/customer-tracker
./deploy-frontend-all-in-one.sh
```

**No password prompts!** 🚀

---

## Troubleshooting

### SSH still asks for password

1. **Check permissions on your local machine:**
```bash
chmod 700 ~/.ssh
chmod 600 ~/.ssh/id_ed25519_ecs
chmod 644 ~/.ssh/id_ed25519_ecs.pub
```

2. **Check server-side permissions:**
```bash
ssh root@47.109.72.216
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

3. **Check server SSH config:**
```bash
sudo nano /etc/ssh/sshd_config
```

Make sure these lines are uncommented:
```
PubkeyAuthentication yes
AuthorizedKeysFile .ssh/authorized_keys
```

Restart SSH:
```bash
sudo systemctl restart sshd
```

### "Permission denied (publickey)"

This means:
- Wrong key file
- Key not copied to server correctly
- Server doesn't allow public key auth

**Solution:** Repeat Step 2 (ssh-copy-id)

### Connection timeout

Check server firewall:
```bash
sudo ufw status
# Should show: 22/tcp ALLOW
```

---

## Security Best Practices

### 1. Use Passphrase (Optional but Recommended)

When generating the key, add a passphrase:
```bash
ssh-keygen -t ed25519 -a 100 -C "your_email@example.com" -f ~/.ssh/id_ed25519_ecs
# Enter passphrase when prompted
```

Then use `ssh-agent` so you don't type it every time:
```bash
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519_ecs
# Enter passphrase once, it's remembered for the session
```

### 2. Disable Password Authentication (After Keys Work)

On the server:
```bash
sudo nano /etc/ssh/sshd_config
```

Change:
```
PasswordAuthentication no
```

Restart SSH:
```bash
sudo systemctl restart sshd
```

**⚠️ Only do this AFTER keys are working and you've tested!**

### 3. Backup Your Keys

```bash
# Copy keys to safe location
cp ~/.ssh/id_ed25519_ecs ~/backup/
cp ~/.ssh/id_ed25519_ecs.pub ~/backup/
```

### 4. Never Share Private Key

- ❌ Don't email the private key (id_ed25519_ecs)
- ❌ Don't upload it to cloud storage
- ✅ Only share the public key (id_ed25519_ecs.pub)

---

## Quick Reference

**Generate key:**
```bash
ssh-keygen -t ed25519 -a 100 -f ~/.ssh/id_ed25519_ecs
```

**Copy to server:**
```bash
ssh-copy-id -i ~/.ssh/id_ed25519_ecs.pub root@47.109.72.216
```

**Test login:**
```bash
ssh -i ~/.ssh/id_ed25519_ecs root@47.109.72.216
```

**Deploy (after setup):**
```bash
cd /Users/admin/WorkSpace/ai/customer-tracker
./deploy-frontend-all-in-one.sh
```

---

## Summary

With SSH keys setup:
- ✅ Automated deployments work without password prompts
- ✅ More secure than passwords
- ✅ Faster deployment workflow
- ✅ Can be used for automated CI/CD pipelines

**Time to setup:** 5 minutes
**Time saved:** Every deployment afterwards! 🚀
